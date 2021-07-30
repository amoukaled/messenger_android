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

package com.example.messenger.data.local.relations

import androidx.room.Embedded
import androidx.room.Relation

import com.example.messenger.data.local.entities.Contact
import com.example.messenger.data.local.entities.Message
import com.example.messenger.utils.Constants

/**
 * The SQL relation between [Contact] and [Message].
 */
data class ContactWithMessages(
    @Embedded
    val contact: Contact,

    @Relation(
        parentColumn = "phoneNumber",
        entityColumn = "contactOwner"
    )
    var messages: List<Message>
) {

    // Sort the messages by the timestamp.
    init {
        messages = messages.sortedBy { it.timestamp }
    }

    /**
     * @return The timestamp of the most recent message.
     */
    fun getLatestMessageTimestamp(): Long {
        var message: Long = 0

        for (mes in messages) {
            if (mes.timestamp >= message) {
                message = mes.timestamp
            }
        }

        return message
    }

    /**
     * Whether the [Contact] is active on Firebase Cloud Messaging.
     */
    fun isContactActive(): Boolean {
        // Using null-safe callbacks
        contact.token?.let {
            return true
        }

        return false
    }

    /**
     * Whether the [messages] list is empty or not.
     */
    fun isChatEmpty() = messages.isEmpty()

    /**
     * @return The latest message string.
     */
    fun getLatestMessage(): String? {
        var message: Message? = null

        for (mes in messages) {
            if (message == null) {
                message = mes
            }
            if (mes.timestamp >= message.timestamp) {
                message = mes
            }
        }
        // TODO Add all branches
        return when (message?.messageType) {
            Constants.TEXT_MESSAGE -> {
                message.data
            }
            Constants.IMAGE_MESSAGE -> {
                "Image"
            }
            else -> {
                null
            }
        }
    }


    /**
     * @return The count of unread messages.
     */
    fun getUnreadMessageCount(): Int = messages.count { !it.isRead }

}
