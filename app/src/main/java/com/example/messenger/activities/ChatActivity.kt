/* Copyright (C) 2021  Ali Moukaled
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.messenger.activities

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.messenger.R
import com.example.messenger.adapters.MessageBubbleAdapter
import com.example.messenger.data.local.entities.Contact
import com.example.messenger.data.local.entities.Message
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.databinding.ActivityChatBinding
import com.example.messenger.fragments.ChatParticipantInfoFragment
import com.example.messenger.fragments.ContactErrorFragment
import com.example.messenger.fragments.MessageInputFragment
import com.example.messenger.models.ChatEvent
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.repositories.AuthRepository
import com.example.messenger.repositories.MessagingRepository
import com.example.messenger.utils.Constants
import com.example.messenger.viewModels.ChatActivityViewModel

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.flow.collect

import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    // DI
    @Inject
    lateinit var messagingRepository: MessagingRepository

    @Inject
    lateinit var dispatchers: DispatcherProvider

    @Inject
    lateinit var authRepo: AuthRepository

    @Inject
    lateinit var remoteStorage: RemoteStorage

    // Variables
    private lateinit var binding: ActivityChatBinding
    private lateinit var phoneNumber: String
    private lateinit var bubbleAdapter: MessageBubbleAdapter

    // the message count to load into the RV to reduce memory usage
    private var loadMessageCount = 20

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var model: ChatActivityViewModel
    private lateinit var messageInputFragment: MessageInputFragment
    private lateinit var contactErrorFragment: ContactErrorFragment

    // Scroll listener to load previous messages on scroll.
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            layoutManager.findFirstVisibleItemPosition().let {
                val newMessages = model.chat.value.data?.messages
                if (it == 0 && newMessages != null && loadMessageCount > 0) {
                    bubbleAdapter.refreshAdapter(getLimitedList(newMessages))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (intent.extras?.get(Constants.CHAT_ID) as String).let {
            this.phoneNumber = it
        }

        model = ChatActivityViewModel(phoneNumber, messagingRepository, dispatchers, authRepo, remoteStorage)
        model.addSnapshotListener(this, phoneNumber)
        messageInputFragment = MessageInputFragment(model)
        contactErrorFragment = ContactErrorFragment(null)

        initBackButton()
        initInfoFragment()
        lifecycleScope.launchWhenCreated {
            collectEvent(model)
        }
    }

    /**
     * Initializes the Chat info fragment.
     */
    private fun initInfoFragment() {
        binding.chatTitleTV.setOnClickListener {
            ChatParticipantInfoFragment(model).apply {
                show(supportFragmentManager, "chatInfo")
            }
        }
    }

    /**
     * Initializes the back button.
     */
    private fun initBackButton() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Returns a sublist of [Message] specified by the [loadMessageCount].
     */
    private fun getLimitedList(list: List<Message>): List<Message> {
        return if (list.size - loadMessageCount < 20) {
            list
        } else {
            loadMessageCount += 20
            val from = list.size - loadMessageCount
            val to = list.size
            list.subList(from, to)
        }
    }

    /**
     * Initializes the Messages RecyclerView.
     * If [bubbleAdapter] is initialized, it won't reinitialize
     * the RV.
     */
    private fun initMessagesRV(chat: ContactWithMessages) {
        if (!this::bubbleAdapter.isInitialized) {
            val limitedList = getLimitedList(chat.messages)
            bubbleAdapter = MessageBubbleAdapter(limitedList, model)
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            with(binding) {
                messagesRV.adapter = bubbleAdapter
                messagesRV.layoutManager = layoutManager
                messagesRV.addOnScrollListener(scrollListener)
            }
            initAvatar(chat.contact)
            model.updateChatOnOpen(phoneNumber, chat.contact.name)
        }
        binding.messagesRV.scrollToPosition(chat.messages.size - 1)
    }

    /**
     * Collects chat events and update UI accordingly.
     */
    private suspend fun collectEvent(model: ChatActivityViewModel) {
        model.chat.collect { chat ->

            when (chat) {
                is ChatEvent.Loading -> {
                    // only change visibility when needed
                    binding.apply {
                        if (chatActivityTB.isVisible) {
                            chatActivityTB.isGone = true
                        }
                        if (messagesRV.isVisible) {
                            messagesRV.isGone = true
                        }
                        if (chatPB.isGone) {
                            chatPB.isVisible = true
                        }
                        if (chatActivityBottomFL.isVisible) {
                            chatActivityBottomFL.isGone = true
                        }
                    }
                }
                is ChatEvent.Data -> {
                    chat.data?.let { nonNullChat ->
                        binding.apply {
                            if (chatActivityTB.isGone) {
                                chatActivityTB.isVisible = true
                            }
                            if (messagesRV.isGone) {
                                messagesRV.isVisible = true
                            }
                            if (chatPB.isVisible) {
                                chatPB.isGone = true
                            }

                            // If participant not active or blocked, hide the input EditText
                            if (nonNullChat.isContactActive() && !model.isContactBlocked(nonNullChat.contact.phoneNumber)) {
                                if (chatActivityBottomFL.isGone) {
                                    chatActivityBottomFL.isVisible = true
                                }

                                if (nonNullChat.isChatEmpty()) {
                                    if (emptyMessagesIV.root.isGone) {
                                        emptyMessagesIV.root.isVisible = true
                                    }
                                } else {
                                    if (emptyMessagesIV.root.isVisible) {
                                        emptyMessagesIV.root.isGone = true
                                    }
                                }

                                // hiding and removing the appropriate fragment from supportManager stack
                                supportFragmentManager.apply {
                                    val isContactErrorInLayout =
                                        findFragmentById(contactErrorFragment.id)?.isInLayout == true
                                    val isMessageInputNotInLayout =
                                        findFragmentById(messageInputFragment.id)?.isInLayout == false
                                    beginTransaction().apply {
                                        if (isContactErrorInLayout) {
                                            remove(messageInputFragment)
                                        }
                                        if (isMessageInputNotInLayout) {
                                            replace(
                                                chatActivityBottomFL.id,
                                                messageInputFragment
                                            )
                                        }

                                        if (isContactErrorInLayout || isMessageInputNotInLayout) {
                                            commit()
                                        }
                                    }
                                }
                            } else {
                                if (chatActivityBottomFL.isGone) {
                                    chatActivityBottomFL.isVisible = true
                                }
                                if (emptyMessagesIV.root.isVisible) {
                                    emptyMessagesIV.root.isGone = true
                                }
                                supportFragmentManager.apply {
                                    val isMessageInputInLayout =
                                        findFragmentById(messageInputFragment.id)?.isInLayout == true
                                    val isContactErrorNotInLayout =
                                        findFragmentById(contactErrorFragment.id)?.isInLayout == false
                                    beginTransaction().apply {
                                        if (isMessageInputInLayout) {
                                            remove(messageInputFragment)
                                        }
                                        if (isContactErrorNotInLayout) {
                                            contactErrorFragment.message =
                                                if (model.isContactBlocked(nonNullChat.contact.phoneNumber)) getString(
                                                    R.string.contact_blocked
                                                ) else
                                                    getString(R.string.contact_not_receiving)
                                            replace(chatActivityBottomFL.id, contactErrorFragment)
                                        }

                                        if (isMessageInputInLayout || isContactErrorNotInLayout) {
                                            commit()
                                        }
                                    }
                                }
                            }

                            chatTitleTV.text = nonNullChat.contact.name
                                ?: nonNullChat.contact.phoneNumber
                            model.makeMessagesRead(nonNullChat)
                            initMessagesRV(nonNullChat)
                            bubbleAdapter.refreshAdapter(nonNullChat.messages)
                        }
                    }
                }
            }
        }
    }

    /**
     * Initializes the avatar according to the size in the layout.
     */
    private fun initAvatar(contact: Contact) {
        contact.getProfilePicRoundedDrawable(
            this,
            Constants.SMALL_AVATAR_WIDTH,
            Constants.SMALL_AVATAR_HEIGHT
        )
            .let {
                binding.chatAvatarIV.setImageDrawable(it)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.messagesRV.removeOnScrollListener(scrollListener)
    }

}