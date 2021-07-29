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

package com.example.messenger.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.example.messenger.R
import com.example.messenger.activities.ChatActivity
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.databinding.ChatItemBinding
import com.example.messenger.utils.Constants
import com.example.messenger.viewModels.MainActivityViewModel

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChatItemAdapter(
    private var chats: MutableList<ContactWithMessages>,
    private val model: MainActivityViewModel?
) :
    RecyclerView.Adapter<ChatItemAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat, this::deleteChat)
    }

    override fun getItemCount(): Int = chats.size


    // Diff Utils
    class ChatItemDiffUtil(
        private val oldList: List<ContactWithMessages>,
        private val newList: List<ContactWithMessages>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].contact.phoneNumber == newList[newItemPosition].contact.phoneNumber
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    /**
     * Deletes the chat using the [MainActivityViewModel].
     */
    private fun deleteChat(chat: ContactWithMessages) {
        model?.deleteChat(chat)
    }

    /**
     * Updates the current list by using the [ChatItemDiffUtil].
     */
    fun updateAdapter(list: List<ContactWithMessages>) {
        val oldList = this.chats

        val result: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            ChatItemDiffUtil(
                oldList,
                list
            )
        )

        chats = list.toMutableList()
        result.dispatchUpdatesTo(this)
    }

    /**
     * Force refreshes the adapter.
     */
    fun forceRefresh(list: List<ContactWithMessages>) {
        chats.clear()
        chats.addAll(list)
        this.notifyDataSetChanged()
    }


    /**
     * ViewHolder class.
     * @property binding The view binding class for the layout.
     */
    class ChatViewHolder(private val binding: ChatItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the [chat] data to the view.
         * @param chat The [ContactWithMessages] object.
         * @param deleteChat Callback to delete the chat in the RV.
         */
        fun bind(chat: ContactWithMessages, deleteChat: (ContactWithMessages) -> Unit) {
            with(binding) {

                // Contact Name
                contactNameTV.text = chat.contact.name ?: chat.contact.phoneNumber

                // Latest message preview
                chat.getLatestMessage()?.let {
                    messagePreviewTV.text = it
                }

                // Unread indicator
                chat.getUnreadMessageCount().let {
                    if (it == 0) {
                        unreadCountTV.isInvisible = true
                    } else {
                        unreadCountTV.isVisible = true
                        unreadCountTV.text = it.toString()
                    }
                }


                // Contact Avatar.
                chat.contact.getProfilePicRoundedDrawable(
                    contactNameTV.context,
                    Constants.MEDIUM_AVATAR_WIDTH,
                    Constants.MEDIUM_AVATAR_HEIGHT,
                ).let {
                    contactAvatarIV.setImageDrawable(it)
                }

                // Delete callback
                root.setOnLongClickListener { root ->
                    MaterialAlertDialogBuilder(root.context).setTitle(
                        root.context.getString(
                            R.string.delete_chat,
                            chat.contact.name
                        )
                    )
                        .setPositiveButton(R.string.delete) { dialog, _ ->
                            deleteChat(chat)
                            dialog.cancel()
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                    return@setOnLongClickListener true
                }

                // Opens the chat in a new activity.
                root.setOnClickListener {
                    Intent(it.context, ChatActivity::class.java).apply {
                        putExtra(Constants.CHAT_ID, chat.contact.phoneNumber)
                        it.context.startActivity(this)
                    }
                }
            }
        }
    }

}