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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.messenger.models.ChatEvent
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.models.NotificationData
import com.example.messenger.models.PushNotification
import com.example.messenger.repositories.AuthRepository
import com.example.messenger.repositories.MessagingRepository
import com.example.messenger.utils.Constants
import com.example.messenger.utils.ImageUtil
import com.example.messenger.utils.InternalStorageHelper

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChatActivityViewModel(
    private val phoneNumber: String,
    private val messagingRepository: MessagingRepository,
    private val dispatchers: DispatcherProvider,
    private val authRepository: AuthRepository,
    private val remoteStorage: RemoteStorage
) : ViewModel() {

    private val _chat = MutableStateFlow<ChatEvent>(ChatEvent.Loading())
    val chat: StateFlow<ChatEvent> = _chat

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
    fun sendTextMessage(token: String, message: String, phoneNumber: String) {
        authRepository.getCurrentUser()?.phoneNumber?.let { userNum ->
            val notification =
                PushNotification(
                    NotificationData(
                        userNum,
                        message,
                        Constants.TEXT_MESSAGE,
                        null,
                        null,
                        null,
                        null
                    ), token
                )
            viewModelScope.launch(dispatchers.io) {
                messagingRepository.sendTextMessage(notification, phoneNumber)
            }
        }
    }

    fun sendImageMessage(
        token: String,
        message: String?,
        uri: Uri,
        phoneNumber: String,
        context: Context
    ) {
        authRepository.getCurrentUser()?.phoneNumber?.let { userNumber ->
            viewModelScope.launch(dispatchers.io) {
                kotlin.runCatching {
                    context.contentResolver.openInputStream(uri)?.let { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        val preview = ImageUtil.getHexFromBitmap(bitmap)
                        val notification =
                            PushNotification(
                                NotificationData(
                                    userNumber,
                                    message,
                                    Constants.IMAGE_MESSAGE,
                                    null,
                                    preview,
                                    bitmap.width,
                                    bitmap.height
                                ), token
                            )
                        messagingRepository.sendImageMessage(
                            notification,
                            bitmap,
                            phoneNumber,
                            context
                        )
                    }
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

    fun downloadImage(imageId: String, context: Context) {
        viewModelScope.launch(dispatchers.io) {
            remoteStorage.downloadChatMedia(imageId)?.let {
                InternalStorageHelper.saveImageToAppStorage(
                    it,
                    imageId,
                    context,
                    InternalStorageHelper.CHAT_MEDIA_DIR
                )
            }
            messagingRepository.refreshChats()
        }
    }

}