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

import com.example.messenger.api.MessagingApi
import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.local.entities.BlockedContact
import com.example.messenger.data.local.entities.Contact
import com.example.messenger.data.local.entities.Message
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.models.PushNotification
import com.example.messenger.utils.*

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.messaging.RemoteMessage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import java.util.*

class DefaultMessagingRepository(
    private val api: MessagingApi,
    private val contactDao: ContactDao,
    private val remoteDao: RemoteDao,
    private val contactsHelper: ContactsHelper,
    private val remoteStorage: RemoteStorage
) :
    MessagingRepository {

    private val _chats = MutableStateFlow(mutableListOf<ContactWithMessages>())
    override val chats: StateFlow<List<ContactWithMessages>> = _chats


    /**
     * Initializes the repo flow.
     * Gets all [ContactWithMessages] and filters out the ones
     * with empty messages list.
     */
    override suspend fun initMessagingRepo() {
        refreshChats()
    }


    override suspend fun sendTextMessage(notification: PushNotification, phoneNumber: String) {
        if (!isContactBlocked(phoneNumber) && notification.data.message != null) {
            val message = Message(
                notification.data.message,
                false,
                phoneNumber,
                null,
                true,
                Calendar.getInstance().timeInMillis,
                Constants.TEXT_MESSAGE,
                null
            )
            try {
                contactDao.insertMessage(message).also {
                    message.id = it
                }
                refreshChats()
                val res = api.sendMessage(notification)

                if (res.isSuccessful) {
                    message.isSent = true
                    contactDao.updateMessage(message)
                    refreshChats()
                } else {
                    message.isSent = false
                    contactDao.updateMessage(message)
                    refreshChats()
                }
            } catch (e: Exception) {
                message.isSent = false
                contactDao.updateMessage(message)
                refreshChats()
            }
        }
    }

    override suspend fun sendImageMessage( // TODO workManager
        notification: PushNotification, bitmap: Bitmap, phoneNumber: String, context: Context
    ) {
        if (!isContactBlocked(phoneNumber)) {
            // Generate new id
            val imageId = idGenerator()

            // Save locally
            InternalStorageHelper.saveImageToAppStorage(
                bitmap, imageId, context,
                InternalStorageHelper.CHAT_MEDIA_DIR
            )

            // Save Message instance
            val message = Message(
                notification.data.message,
                false,
                phoneNumber,
                null,
                true,
                Calendar.getInstance().timeInMillis,
                Constants.IMAGE_MESSAGE,
                imageId
            )
            contactDao.insertMessage(message).let {
                message.id = it
            }
            refreshChats()

            // Save remotely
            try {
                val res = remoteStorage.uploadChatMedia(bitmap, imageId)

                if (res) {
                    // Send message
                    notification.data.image = imageId
                    val result = api.sendMessage(notification)
                    message.isSent = result.isSuccessful
                    contactDao.updateMessage(message)
                    refreshChats()
                    return
                } else {
                    throw Exception()
                }
            } catch (e: Exception) {
                message.isSent = false
                contactDao.updateMessage(message)
                refreshChats()
                return
            }
        }
    }

    override suspend fun getAllChats(): List<ContactWithMessages> =
        contactDao.getAllContactWithMessages()

    override suspend fun deleteChatMessages(contactWithMessages: ContactWithMessages) {
        val ids = contactWithMessages.messages.map { it.id }
        contactDao.deleteMessages(ids)
        if (contactWithMessages.contact.name == null) {
            contactDao.deleteContact(contactWithMessages.contact)
        }
        refreshChats()
    }

    override suspend fun makeMessagesRead(contactWithMessages: ContactWithMessages) {
        val unread = contactWithMessages.messages.filter { !it.isRead }
        for (mes in unread) {
            mes.isRead = true
            contactDao.updateMessage(mes)
        }
        refreshChats()
    }

    override suspend fun contactExists(phoneNumber: String): Boolean =
        contactDao.contactExists(phoneNumber)

    override suspend fun receiveMessageWithContact(
        messageData: String?, from: String,
        timestamp: Long, messageType: Int,
        imageLink: String?
    ) {
        Message(messageData, true, from, null, false, timestamp, messageType, imageLink).also {
            contactDao.insertMessage(it)
            refreshChats()
        }
    }

    override suspend fun receiveMessageWithoutContact(
        messageData: String?, from: String,
        timestamp: Long, token: String,
        messageType: Int, imageLink: String?
    ) {
        val fsContact = remoteDao.getAvailableContactByNumber(from)
        val status = fsContact.data?.status

        val contact = Contact(from, null, token, status)
        contactDao.insertContact(contact)

        val message =
            Message(messageData, true, from, null, false, timestamp, messageType, imageLink)
        contactDao.insertMessage(message)
        refreshChats()
    }

    override suspend fun updateChatOnOpen(number: String, name: String?) {
        contactsHelper.updateContact(number, name)
        refreshChats()
    }


    /**
     * Refreshes the flow.
     */
    override suspend fun refreshChats() {
        val messages = contactDao.getAllContactWithMessages().let { list ->
            list.sortedByDescending { it.getLatestMessageTimestamp() }
        }
        messages.forEach { contact ->
            contact.messages.sortedBy { it.timestamp }
        }
        _chats.emit(messages.toMutableList()) // TODO check
    }

    override suspend fun updateContactsFromPhoneContacts() {
        try {
            contactsHelper.updateContactsFromPhoneContacts()
            updateBlockList()
            refreshChats()
        } catch (e: Exception) {

        }
    }

    override suspend fun updateDBContacts() {
        try {
            contactsHelper.updateDBContacts()
            updateBlockList()
            refreshChats()
        } catch (e: Exception) {
        }
    }

    override suspend fun isContactBlocked(phoneNumber: String): Boolean =
        contactDao.blockedContactExists(phoneNumber)

    override suspend fun blockContact(phoneNumber: String) {
        val res = remoteDao.addBlockedContact(phoneNumber)
        if (res) {
            contactDao.insertBlockedContact(BlockedContact(phoneNumber))
            refreshChats()
        }
    }

    override suspend fun unblockContact(phoneNumber: String) {
        val res = remoteDao.removeBlockedContact(phoneNumber)
        if (res) {
            contactDao.deleteBlockedContact(BlockedContact(phoneNumber))
            refreshChats()
        }
    }

    override fun addSnapshotListenerOnUser(
        activity: Activity,
        phoneNumber: String,
        eventListener: EventListener<DocumentSnapshot?>
    ) {
        remoteDao.getSpecificUserDocumentReference(phoneNumber)
            .addSnapshotListener(activity, eventListener)
    }

    override suspend fun updateContactFromDocSnapshot(snapshot: DocumentSnapshot) {
        snapshot.data?.let { data ->
            (data["token"] as String?).let { token ->
                (data["status"] as String?)?.let { status ->
                    val number = snapshot.id

                    contactDao.getContactByPhoneNumber(number)?.let {
                        it.token = token
                        it.status = status
                        contactDao.updateContact(it)
                        refreshChats()
                    }
                }
            }
        }
    }

    override suspend fun updateBlockList() {
        remoteDao.getBlockList().data?.let {
            for (contact in it) {
                contactDao.insertBlockedContact(contact)
            }
        }
    }

    override suspend fun receiveRemoteMessage(context: Context, message: RemoteMessage) {
        val timestamp = message.sentTime

        message.data.apply {
            get("messageType")?.let { messageTypeStr ->
                val messageType = try {
                    messageTypeStr.toInt()
                } catch (e: Exception) {
                    return
                }


                when (messageType) {
                    Constants.TEXT_MESSAGE -> {
                        get("message")?.let { messageData ->
                            get("title")?.let { phoneNumber ->

                                val isBlocked =
                                    this@DefaultMessagingRepository.isContactBlocked(phoneNumber)

                                if (!isBlocked) {
                                    val contactExists =
                                        this@DefaultMessagingRepository.contactExists(phoneNumber)

                                    if (contactExists) {
                                        this@DefaultMessagingRepository.receiveMessageWithContact(
                                            messageData, phoneNumber,
                                            timestamp, messageType, null
                                        )
                                    } else {
                                        remoteDao.getTokenFromPhoneNumber(phoneNumber)
                                            ?.let { token ->
                                                this@DefaultMessagingRepository.receiveMessageWithoutContact(
                                                    messageData, phoneNumber,
                                                    timestamp, token, messageType, null
                                                )
                                            }
                                    }

                                    NotificationHelper.apply {
                                        val isAppRunning = isAppRunning(context)
                                        if (!isAppRunning) {
                                            val messageTitle =
                                                contactDao.getContactByPhoneNumber(phoneNumber)?.name
                                                    ?: phoneNumber

                                            displayNotification(context, messageTitle, messageData)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Constants.IMAGE_MESSAGE -> {
                        get("message").let { messageData ->
                            get("title")?.let { phoneNumber ->
                                get("image")?.let { image ->
                                    get("imagePreview")?.let { imagePreview ->
                                        get("imageWidth")?.let { width ->
                                            get("imageHeight")?.let { height ->
                                                val imageWidth = try {
                                                    width.toInt()
                                                } catch (e: Exception) {
                                                    return
                                                }
                                                val imageHeight = try {
                                                    height.toInt()
                                                } catch (e: Exception) {
                                                    return
                                                }

                                                val isBlocked =
                                                    this@DefaultMessagingRepository.isContactBlocked(
                                                        phoneNumber
                                                    )

                                                if (!isBlocked) {
                                                    val contactExists =
                                                        this@DefaultMessagingRepository.contactExists(
                                                            phoneNumber
                                                        )

                                                    // Save preview
                                                    val decodedImage =
                                                        ImageUtil.getBitmapFromHex(
                                                            imagePreview,
                                                            imageWidth,
                                                            imageHeight
                                                        )
                                                    InternalStorageHelper.saveImageToAppStorage(
                                                        decodedImage, image, context,
                                                        InternalStorageHelper.CHAT_MEDIA_PREVIEW_DIR
                                                    )


                                                    if (contactExists) {
                                                        this@DefaultMessagingRepository.receiveMessageWithContact(
                                                            messageData, phoneNumber,
                                                            timestamp, messageType, image
                                                        )
                                                    } else {
                                                        remoteDao.getTokenFromPhoneNumber(
                                                            phoneNumber
                                                        )
                                                            ?.let { token ->
                                                                this@DefaultMessagingRepository.receiveMessageWithoutContact(
                                                                    messageData,
                                                                    phoneNumber,
                                                                    timestamp,
                                                                    token,
                                                                    messageType,
                                                                    image
                                                                )
                                                            }
                                                    }

                                                    NotificationHelper.apply {
                                                        val isAppRunning = isAppRunning(context)
                                                        if (!isAppRunning) {
                                                            val messageTitle =
                                                                contactDao.getContactByPhoneNumber(
                                                                    phoneNumber
                                                                )?.name
                                                                    ?: phoneNumber

                                                            messageData?.let { data ->
                                                                displayNotification(
                                                                    context, messageTitle, data
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        return
                    }
                }
            }
        }
    }

}