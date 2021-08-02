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

package com.example.messenger.viewModels

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.local.entities.Message
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.models.ChatEvent
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.repositories.MessagingRepository
import com.example.messenger.utils.Constants
import com.example.messenger.utils.InternalStorageHelper
import com.example.messenger.utils.idGenerator
import com.example.messenger.workers.SendImageMessageWorker
import com.example.messenger.workers.SendTextMessageWorker
import com.example.messenger.workers.WorkerDataConstants

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

import java.util.*

class ChatActivityViewModel(
    private val phoneNumber: String,
    private val messagingRepository: MessagingRepository,
    private val dispatchers: DispatcherProvider,
    private val remoteStorage: RemoteStorage,
    private val contactDao: ContactDao,
    context: Context
) : ViewModel() {

    private val _chat = MutableStateFlow<ChatEvent>(ChatEvent.Loading())
    val chat: StateFlow<ChatEvent> = _chat
    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch(dispatchers.io) {
            messagingRepository.chats.transform { chats ->
                val singleChat =
                    chats.find { it.contact.phoneNumber == this@ChatActivityViewModel.phoneNumber }
                emit(singleChat)
            }.collect {
                if (it == null) {
                    _chat.emit(ChatEvent.Loading())
                } else {
                    _chat.emit(ChatEvent.Data(it))
                }
            }
        }
    }

    private suspend fun refreshChatFlow() {
        val chat =
            messagingRepository.chats.value.find { it.contact.phoneNumber == this@ChatActivityViewModel.phoneNumber }
        if (chat == null) {
            _chat.emit(ChatEvent.Loading())
        } else {
            _chat.emit(ChatEvent.Data(chat))
        }
    }

    /**
     * Sets all unread messages to read.
     */
    fun makeMessagesRead(chat: ContactWithMessages, scope: CoroutineScope = viewModelScope) {
        scope.launch(dispatchers.io) {
            messagingRepository.makeMessagesRead(chat)
        }
    }

    /**
     * Sends a message.
     */
    fun sendTextMessage(message: String, phoneNumber: String) {
        if (!isContactBlocked(phoneNumber)) {
            viewModelScope.launch(dispatchers.io) {
                val messageEntity = Message(
                    message,
                    false,
                    phoneNumber,
                    null,
                    true,
                    Calendar.getInstance().timeInMillis,
                    Constants.TEXT_MESSAGE,
                    null
                )

                contactDao.insertMessage(messageEntity).also {
                    messageEntity.id = it
                }
                messagingRepository.refreshChats()
                val data = Data.Builder().apply {
                    putLong(WorkerDataConstants.MESSAGE_ID_KEY, messageEntity.id)
                    putString(WorkerDataConstants.PHONE_NUM_KEY, phoneNumber)
                }
                val req =
                    OneTimeWorkRequestBuilder<SendTextMessageWorker>().setInputData(data.build())
                        .build()
                workManager.beginWith(req).enqueue()
            }
        }
    }

    fun sendImageMessage(
        message: String?,
        path: String, phoneNumber: String, context: Context
    ) {
        if (!isContactBlocked(phoneNumber)) {
            viewModelScope.launch(dispatchers.io) {
                // Save locally
                BitmapFactory.decodeFile(path)?.let { bitmap ->
                    // Generate new id
                    val imageId = idGenerator()
                    InternalStorageHelper.saveImageToAppStorage(
                        bitmap, imageId, context,
                        InternalStorageHelper.CHAT_MEDIA_DIR
                    )

                    // Save Message instance
                    val messageEntity = Message(
                        message,
                        false,
                        phoneNumber,
                        null,
                        true,
                        Calendar.getInstance().timeInMillis,
                        Constants.IMAGE_MESSAGE,
                        imageId
                    )
                    contactDao.insertMessage(messageEntity).let {
                        messageEntity.id = it
                    }
                    messagingRepository.refreshChats()

                    val data = Data.Builder().apply {
                        putString(WorkerDataConstants.PHONE_NUM_KEY, phoneNumber)
                        putString(WorkerDataConstants.LOCAL_IMAGE_ID_KEY, imageId)
                        putLong(WorkerDataConstants.MESSAGE_ID_KEY, messageEntity.id)
                    }
                    val req =
                        OneTimeWorkRequestBuilder<SendImageMessageWorker>().setInputData(data.build())
                            .build()
                    workManager.beginWith(req).enqueue()
                }
            }
        }


    }

    fun updateChatOnOpen(number: String, name: String?) {
        viewModelScope.launch(dispatchers.io) {
            messagingRepository.updateChatOnOpen(number, name)
        }
    }

    fun isContactBlocked(phoneNumber: String): Boolean {
        return runBlocking {
            return@runBlocking messagingRepository.isContactBlocked(phoneNumber)
        }
    }

    fun blockContact(number: String) {
        viewModelScope.launch(dispatchers.io) {
            _chat.emit(ChatEvent.Loading())
            messagingRepository.blockContact(number)
            refreshChatFlow()
        }
    }

    fun unblockContact(number: String) {
        viewModelScope.launch(dispatchers.io) {
            _chat.emit(ChatEvent.Loading())
            messagingRepository.unblockContact(number)
            refreshChatFlow()
        }
    }

    fun addSnapshotListener(activity: Activity, phoneNumber: String) {
        messagingRepository.addSnapshotListenerOnUser(activity, phoneNumber, eventListener)
    }

    private fun updateContactFromDocSnapshot(snapshot: DocumentSnapshot) {
        viewModelScope.launch(dispatchers.io) {
            messagingRepository.updateContactFromDocSnapshot(snapshot)
        }
    }

    private val eventListener =
        EventListener<DocumentSnapshot?> { value, _ ->
            value?.let { snapshot ->
                updateContactFromDocSnapshot(snapshot)
            }
        }

    // TODO work manager
    fun downloadImage(imageId: String, context: Context, refresh: (() -> Unit)?) {
        viewModelScope.launch(dispatchers.io) {
            remoteStorage.downloadChatMedia(imageId)?.let {
                InternalStorageHelper.saveImageToAppStorage(
                    it,
                    imageId,
                    context,
                    InternalStorageHelper.CHAT_MEDIA_DIR
                )
            }
            withContext(dispatchers.main) {
                refresh?.invoke()
            }
        }
    }

}