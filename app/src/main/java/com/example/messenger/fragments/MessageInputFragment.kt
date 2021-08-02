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

package com.example.messenger.fragments

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import com.example.messenger.data.local.entities.Contact
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.databinding.FragmentMessageInputBinding
import com.example.messenger.models.ChatEvent
import com.example.messenger.utils.getRealPathFromUri
import com.example.messenger.viewModels.ChatActivityViewModel

import kotlinx.coroutines.flow.collect

class MessageInputFragment(private val model: ChatActivityViewModel) : Fragment() {

    private var binding: FragmentMessageInputBinding? = null
    private lateinit var getImage: ActivityResultLauncher<IntentSenderRequest>
    private val onInputChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            s?.let {
                if (it.isBlank()) {
                    if (binding?.attachImageButton?.isGone == true) {
                        binding?.attachImageButton?.isVisible = true
                    }
                } else {
                    if (binding?.attachImageButton?.isVisible == true) {
                        binding?.attachImageButton?.isGone = true
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getImage =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                activityResult?.data?.data?.let { uri ->
                    model.chat.value.data?.let { contact ->
                        if (contact.isContactActive()) {
                            getRealPathFromUri(requireContext(), uri)?.let {
                                model.sendImageMessage(
                                    null, it,
                                    contact.contact.phoneNumber, requireContext()
                                )
                            }
                        }

                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMessageInputBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.userInputET?.addTextChangedListener(onInputChangeListener)

        // If chat has data, indicating that the chat has been initialized, send data,
        // else don't do anything.
        lifecycleScope.launchWhenCreated {
            model.chat.collect { event ->
                when (event) {
                    is ChatEvent.Data -> {
                        event.data?.let { data ->
                            initSendButton(data)
                            initSendImageButton()
                        }
                    }
                    is ChatEvent.Loading -> Unit
                }
            }
        }
    }

    /**
     * Sends a message via FCM.
     */
    private fun initSendButton(contact: ContactWithMessages) {
        binding?.sendMessageButton?.setOnClickListener {
            binding?.userInputET?.text?.let {
                val message = it.toString()

                if (message.isNotBlank() && message.isNotEmpty() && contact.isContactActive()) {
                    model.sendTextMessage(message, contact.contact.phoneNumber)
                    binding?.userInputET?.text?.clear()
                }
            }
        }
    }

    private fun initSendImageButton() {
        binding?.attachImageButton?.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            val pIntent =
                PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val sReq = IntentSenderRequest.Builder(pIntent).build()
            getImage.launch(sReq)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.userInputET?.removeTextChangedListener(onInputChangeListener)
        binding = null
    }
}