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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.example.messenger.R
import com.example.messenger.data.local.entities.Message
import com.example.messenger.databinding.IncomingImageMessageBubbleBinding
import com.example.messenger.databinding.IncomingTextMessageBubbleBinding
import com.example.messenger.databinding.OutgoingImageMessageBubbleBinding
import com.example.messenger.databinding.OutgoingTextMessageBubbleBinding
import com.example.messenger.fragments.ExpandImageFragment
import com.example.messenger.utils.Constants
import com.example.messenger.utils.InternalStorageHelper
import com.example.messenger.utils.formatTimeToString
import com.example.messenger.viewModels.ChatActivityViewModel

import java.util.*

class MessageBubbleAdapter(var messages: List<Message>, private val model: ChatActivityViewModel?) :
    RecyclerView.Adapter<MessageBubbleAdapter.ChatBubbleViewHolder>() {


    /**
     * Returns [ChatBubbleViewHolder] with the appropriate view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatBubbleViewHolder {

        return when (viewType) {
            0 -> {
                ChatBubbleViewHolder(
                    IncomingTextMessageBubbleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).root, model
                )
            }
            1 -> {
                ChatBubbleViewHolder(
                    OutgoingTextMessageBubbleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).root, model
                )
            }
            2 -> {
                ChatBubbleViewHolder(
                    IncomingImageMessageBubbleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).root, model
                )
            }
            3 -> {
                ChatBubbleViewHolder(
                    OutgoingImageMessageBubbleBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).root, model
                )
            }
            else -> {
                throw Exception()
            }
        }
    }

    /**
     * Text message from participant = 0
     * Text message from owner = 1
     * Image message from participant = 2
     * Image message from owner = 3
     */
    override fun getItemViewType(position: Int): Int {
        return when (messages[position].messageType) {
            Constants.TEXT_MESSAGE -> {
                if (messages[position].fromParticipant) {
                    0
                } else {
                    1
                }
            }
            Constants.IMAGE_MESSAGE -> {
                if (messages[position].fromParticipant) {
                    2
                } else {
                    3
                }
            }
            else -> {
                -1
            }
        }
    }

    override fun onBindViewHolder(holder: ChatBubbleViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message, this::forceRefreshAdapter)
    }

    override fun getItemCount(): Int = messages.size

    @SuppressLint("NotifyDataSetChanged")
    private fun forceRefreshAdapter() {
        this.notifyDataSetChanged()
    }

    // Diff Util
    class MessageDiffUtilCallback(
        private val oldMessageList: List<Message>,
        private val newMessageList: List<Message>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldMessageList.size

        override fun getNewListSize(): Int = newMessageList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldMessageList[oldItemPosition].id == newMessageList[newItemPosition].id)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldMessageList[oldItemPosition] == newMessageList[newItemPosition])
        }
    }

    /**
     * Refreshes the adapter using [MessageDiffUtilCallback].
     */
    fun refreshAdapter(data: List<Message>) {
        val oldList = messages
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            MessageDiffUtilCallback(
                oldList,
                data
            )
        )
        messages = data
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * ViewHolder class.
     * @property view The view to inflate.
     */
    class ChatBubbleViewHolder(private val view: View, private val model: ChatActivityViewModel?) :
        RecyclerView.ViewHolder(view) {

        /**
         * Binds the message data to the appropriate viewBinding.
         */
        fun bind(message: Message, refresh: () -> Unit) {
            when (message.messageType) {
                Constants.TEXT_MESSAGE -> {
                    if (message.fromParticipant) {
                        bindIncomingTextMessage(message)
                    } else {
                        bindOutgoingTextMessage(message)
                    }
                }
                Constants.IMAGE_MESSAGE -> {
                    if (message.fromParticipant) {
                        bindIncomingImageMessage(message, refresh)
                    } else {
                        bindOutgoingImageMessage(message)
                    }
                }
                else -> {
                    throw Exception()
                }
            }
        }


        private fun bindIncomingTextMessage(message: Message) {
            IncomingTextMessageBubbleBinding.bind(view).apply {
                participantMessageTV.text = message.data
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
            }
        }

        private fun bindOutgoingTextMessage(message: Message) {
            OutgoingTextMessageBubbleBinding.bind(view).apply {
                userMessageTV.text = message.data

                // Setting the time
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }

                // Setting icon
                message.isSent?.let {
                    if (it) {
                        messageStatusIV.setImageDrawable(
                            ContextCompat.getDrawable(
                                messageStatusIV.context,
                                R.drawable.ic_sent
                            )
                        )
                    } else {
                        messageStatusIV.setImageDrawable(
                            ContextCompat.getDrawable(
                                messageStatusIV.context,
                                R.drawable.ic_error
                            )
                        )
                    }
                    return@apply
                }
                messageStatusIV.setImageDrawable(
                    ContextCompat.getDrawable(
                        messageStatusIV.context,
                        R.drawable.ic_sending
                    )
                )
            }
        }

        private fun bindIncomingImageMessage(message: Message, refresh: () -> Unit) {
            IncomingImageMessageBubbleBinding.bind(view).apply {
                message.imageLink?.let { imageId ->

                    val image = InternalStorageHelper.loadImageFromAppStorage(
                        imageMessageIV.context, imageId,
                        InternalStorageHelper.CHAT_MEDIA_DIR
                    )

                    // If image is downloaded, display it, else display the preview
                    if (image == null) {
                        InternalStorageHelper.loadImageFromAppStorage(
                            messageTimeTV.context,
                            imageId,
                            InternalStorageHelper.CHAT_MEDIA_PREVIEW_DIR
                        )?.let {
                            imageMessageIV.apply {
                                layoutParams.height = it.height
                                layoutParams.width = it.width
                                setImageBitmap(it) // TODO blur
                                requestLayout()
                            }
                        }

                        // Set loading
                        if (imageMessagePB.isVisible) imageMessagePB.isGone = true

                        // Set button
                        downloadImageButton.isVisible = true
                        downloadImageButton.setOnClickListener {
                            it.isGone = true
                            if (imageMessagePB.isGone) imageMessagePB.isVisible = true
                            model?.downloadImage(imageId, it.context, refresh)
                        }
                    } else {
                        // Set the image
                        imageMessageIV.apply {
                            layoutParams.height = image.height
                            layoutParams.width = image.width
                            setImageBitmap(image)
                            requestLayout()
                        }

                        // Hide all buttons
                        if (imageMessagePB.isVisible) imageMessagePB.isGone = true
                        if (downloadImageButton.isVisible) downloadImageButton.isGone = true

                        imageMessageIV.setOnClickListener {
                            ExpandImageFragment().apply {
                                val bundle = Bundle().apply {
                                    putString(
                                        Constants.IMAGE_DIR_NAME_KEY,
                                        InternalStorageHelper.CHAT_MEDIA_DIR
                                    )
                                    putString(Constants.EXPAND_IMAGE_KEY, imageId)
                                }
                                arguments = bundle
                            }.show(
                                (it.context as AppCompatActivity).supportFragmentManager,
                                "expandImage"
                            )
                        }
                    }
                }

                // Setting message data if available
                message.data?.let { messageData ->
                    imageMessageTV.isVisible = true
                    imageMessageTV.text = messageData
                }

                // Setting message time
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
            }
        }

        private fun bindOutgoingImageMessage(message: Message) {
            // TODO re-upload failed message
            OutgoingImageMessageBubbleBinding.bind(view).apply {
                // ImageView
                message.imageLink?.let { imageId ->
                    imageMessageIV.apply {
                        InternalStorageHelper.loadImageFromAppStorage(
                            context,
                            imageId,
                            InternalStorageHelper.CHAT_MEDIA_DIR
                        )?.let { imageBitmap ->
                            layoutParams.height = imageBitmap.height
                            layoutParams.width = imageBitmap.width
                            setImageBitmap(imageBitmap)
                            requestLayout()
                        }
                    }
                }

                // Message data
                message.data?.let { messageData ->
                    imageMessageTV.isVisible = true
                    imageMessageTV.text = messageData
                }

                // Message timestamp
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }

                // If not sending
                message.isSent?.let {
                    if (imageMessagePB.isVisible) imageMessagePB.isGone = true
                    if (it) {
                        // Message sent
                        messageStatusIV.setImageDrawable(
                            ContextCompat.getDrawable(
                                messageStatusIV.context,
                                R.drawable.ic_sent
                            )
                        )

                        // Hide resend button
                        if (resendImageButton.isVisible) resendImageButton.isGone = true

                        // Expand image
                        imageMessageIV.setOnClickListener {
                            // Expand image
                            message.imageLink?.let { imageId ->
                                ExpandImageFragment().apply {
                                    val bundle = Bundle().apply {
                                        putString(
                                            Constants.IMAGE_DIR_NAME_KEY,
                                            InternalStorageHelper.CHAT_MEDIA_DIR
                                        )
                                        putString(Constants.EXPAND_IMAGE_KEY, imageId)
                                    }
                                    arguments = bundle
                                }.show(
                                    (it.context as AppCompatActivity).supportFragmentManager,
                                    "expandImage"
                                )
                            }
                        }
                    } else {
                        // Message not sent, show resend button
                        messageStatusIV.setImageDrawable(
                            ContextCompat.getDrawable(
                                messageStatusIV.context,
                                R.drawable.ic_error
                            )
                        )

                        // Resend Button
                        if (resendImageButton.isGone) resendImageButton.isVisible = true

                        resendImageButton.setOnClickListener {
                            model?.resendImageMessage(message.id)
                        }
                    }
                    return@apply
                }

                // If null set loading
                messageStatusIV.setImageDrawable(
                    ContextCompat.getDrawable(
                        messageStatusIV.context,
                        R.drawable.ic_sending
                    )
                )
                if (imageMessagePB.isGone) imageMessagePB.isVisible = true
                if (resendImageButton.isVisible) resendImageButton.isGone = true
            }
        }
    }

}