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

package com.example.messenger.repositories


import android.app.Activity
import android.content.Context
import android.graphics.Bitmap

import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.models.PushNotification

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.messaging.RemoteMessage

import kotlinx.coroutines.flow.StateFlow

interface MessagingRepository {


    val chats: StateFlow<List<ContactWithMessages>>

    suspend fun initMessagingRepo()

    /**
     * Sends a message via Firebase Cloud Messaging.
     * @param notification The notification to be sent to the user.
     * @param phoneNumber The phone number of the participant.
     */
    suspend fun sendTextMessage(notification: PushNotification, phoneNumber: String)

    /**
     *  Send image message via Firebase Cloud Messaging.
     */
    suspend fun sendImageMessage(
        notification: PushNotification, bitmap: Bitmap, phoneNumber: String, context: Context
    )

    /**
     * Gets all [ContactWithMessages] from [ContactDao].
     */
    suspend fun getAllChats(): List<ContactWithMessages>

    /**
     * Deletes all the messages from the [contactWithMessages] from the db.
     * If the contact name is null, meaning the contact is not saved in the device phonebook,
     * it deletes the contact.
     */
    suspend fun deleteChatMessages(contactWithMessages: ContactWithMessages)

    /**
     * Updates all the unread messages and saves them in the db.
     */
    suspend fun makeMessagesRead(contactWithMessages: ContactWithMessages)

    /**
     * Refreshes the chat state flow.
     */
    suspend fun refreshChats()

    /**
     * Checks whether a contact exists in the Contact table.
     */
    suspend fun contactExists(phoneNumber: String): Boolean

    /**
     * Contact already exists.
     * Adds the message directly to DB and refreshes the flow.
     * @param messageData The message data.
     * @param from The phone number of the user that sent the message.
     * @param timestamp The timestamp of when the message was sent.
     */
    suspend fun receiveMessageWithContact(
        messageData: String?, from: String,
        timestamp: Long, messageType: Int,
        imageLink: String?
    )

    /**
     * Contact doesn't exist.
     * Adds a new contact and refreshes the flow.
     * @param messageData The message data.
     * @param from The phone number of the user that sent the message.
     * @param timestamp The timestamp of when the message was sent.
     * @param token The Firebase Cloud Messaging token of the user that sent
     * the message. Used to create a new Contact.
     */
    suspend fun receiveMessageWithoutContact(
        messageData: String?, from: String,
        timestamp: Long, token: String,
        messageType: Int, imageLink: String?
    )

    /**
     * Updates the chat contact on open and refreshes the flow.
     */
    suspend fun updateChatOnOpen(number: String, name: String?)

    /**
     * Updates the local database contacts.
     * Note: This is used to reduce api calls.
     * @see com.example.messenger.utils.ContactsHelper.updateDBContacts
     */
    suspend fun updateDBContacts()

    /**
     * Updates the local contact database from the device phonebook.
     * Note: This increases the api requests.
     * @see com.example.messenger.utils.ContactsHelper.updateContactsFromPhoneContacts
     */
    suspend fun updateContactsFromPhoneContacts()

    /**
     * Checks whether the contact is blocked.
     */
    suspend fun isContactBlocked(phoneNumber: String): Boolean

    /**
     * Blocks the contact.
     */
    suspend fun blockContact(phoneNumber: String)

    /**
     * Unblocks the contact.
     */
    suspend fun unblockContact(phoneNumber: String)

    /**
     * Updates the block list.
     */
    suspend fun updateBlockList()

    // This is optional and for firebase implementation.
    /**
     * Adds a snapshot listener that would automatically
     * refresh the flow in case the user signs out or changes status.
     */
    fun addSnapshotListenerOnUser(
        activity: Activity,
        phoneNumber: String,
        eventListener: EventListener<DocumentSnapshot?>
    )

    // This is optional and for firebase implementation.
    /**
     * Used to refresh the flow when snapshot is added.
     * @see addSnapshotListenerOnUser
     */
    suspend fun updateContactFromDocSnapshot(snapshot: DocumentSnapshot)

    /**
     * Receives the remote message
     */
    suspend fun receiveRemoteMessage(context: Context, message: RemoteMessage)

}