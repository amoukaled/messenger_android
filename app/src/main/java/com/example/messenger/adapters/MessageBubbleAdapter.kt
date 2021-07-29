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
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

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
        fun bind(message: Message) {
            when (message.messageType) {
                Constants.TEXT_MESSAGE -> {
                    if (message.fromParticipant) {
                        bindTextMessageFromParticipant(message)
                    } else {
                        bindTextMessageFromOwner(message)
                    }
                }
                Constants.IMAGE_MESSAGE -> {
                    if (message.fromParticipant) {
                        bindImageMessageFromParticipant(message)
                    } else {
                        bindImageMessageFromOwner(message)
                    }
                }
                else -> {
                    throw Exception()
                }
            }
        }


        private fun bindTextMessageFromParticipant(message: Message) {
            IncomingTextMessageBubbleBinding.bind(view).apply {
                participantMessageTV.text = message.data
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
            }
        }

        private fun bindTextMessageFromOwner(message: Message) {
            OutgoingTextMessageBubbleBinding.bind(view).apply {
                userMessageTV.text = message.data
                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
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

        private fun bindImageMessageFromParticipant(message: Message) {
            IncomingImageMessageBubbleBinding.bind(view).apply {
                message.imageLink?.let { imageId ->

                    val image = InternalStorageHelper.loadImageFromAppStorage(
                        imageMessageIV.context, imageId,
                        InternalStorageHelper.CHAT_MEDIA_DIR
                    )

                    if (image == null) {
                        InternalStorageHelper.loadImageFromAppStorage(
                            messageTimeTV.context,
                            imageId,
                            InternalStorageHelper.CHAT_MEDIA_PREVIEW_DIR
                        )?.let {
                            imageMessageIV.setImageBitmap(it) // TODO blur
                        }

                        // Set loading
                        if (imageMessagePB.isVisible) imageMessagePB.isGone = true

                        // Set button // TODO force refresh the item in adapter since diffutils wont detect any changes
                        if (downloadImageButton.isGone) {
                            downloadImageButton.isVisible = true
                            downloadImageButton.setOnClickListener {
                                it.isGone = true
                                if (imageMessagePB.isGone) imageMessagePB.isVisible = true
                                model?.downloadImage(imageId, it.context)
                            }
                        }
                    } else {
                        imageMessageIV.setImageBitmap(image)

                        // Hide all buttons
                        if (imageMessagePB.isVisible) imageMessagePB.isGone = true
                        if (downloadImageButton.isVisible) downloadImageButton.isGone = true
                    }

                }

                message.data?.let { messageData ->
                    imageMessageTV.isVisible = true
                    imageMessageTV.text = messageData
                }

                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
            }
        }

        private fun bindImageMessageFromOwner(message: Message) {
            OutgoingImageMessageBubbleBinding.bind(view).apply {
                message.imageLink?.let { imageId ->
                    imageMessageIV.apply {
                        setImageBitmap(
                            InternalStorageHelper.loadImageFromAppStorage(
                                context,
                                imageId,
                                InternalStorageHelper.CHAT_MEDIA_DIR
                            )
                        )
                    }
                }

                message.data?.let { messageData ->
                    imageMessageTV.isVisible = true
                    imageMessageTV.text = messageData
                }

                message.timestamp.also {
                    messageTimeTV.text = formatTimeToString(it)
                }
                message.isSent?.let {
                    if (imageMessagePB.isVisible) {
                        imageMessagePB.isGone = true
                    }
                    if (it) {
                        messageStatusIV.setImageDrawable(
                            ContextCompat.getDrawable(
                                messageStatusIV.context,
                                R.drawable.ic_sent
                            )
                        )

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
                if (imageMessagePB.isGone) {
                    imageMessagePB.isVisible = true
                }
            }
        }

    }

}