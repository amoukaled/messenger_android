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

package com.example.messenger.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @property isSent: true -> Sent; false -> failed; null -> Sending || it is a received message.
 * @property contactOwner The owner of the chat, not the app user.
 * @property fromParticipant Whether the message is sent from the [contactOwner].
 * @property timestamp The timestamp of the message.
 * @property imageLink The link or id of the image. When using Firebase Storage, this refers to the id.
 */
@Entity
data class Message(
    @ColumnInfo(name = "MessageData")
    val data: String?,

    @ColumnInfo(name = "FromParticipant")
    val fromParticipant: Boolean,

    @ColumnInfo(name = "contactOwner")
    val contactOwner: String,

    @ColumnInfo(name = "IsSent")
    var isSent: Boolean?,

    @ColumnInfo(name = "IsSeen")
    var isRead: Boolean,

    @ColumnInfo(name = "Timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "MessageType")
    val messageType: Int,

    @ColumnInfo(name = "ImageLink")
    val imageLink: String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}