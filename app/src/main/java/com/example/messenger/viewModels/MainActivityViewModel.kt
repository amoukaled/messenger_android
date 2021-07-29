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
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.repositories.AuthRepository
import com.example.messenger.repositories.MessagingRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

class MainActivityViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider,
) :
    ViewModel() {

    init {
        viewModelScope.launch(dispatchers.io) {
            messagingRepository.initMessagingRepo()
        }
    }

    val chats: StateFlow<List<ContactWithMessages>> = messagingRepository.chats

    /**
     * Deletes the messages of [ContactWithMessages].
     */
    fun deleteChat(chat: ContactWithMessages, scope: CoroutineScope = viewModelScope) {
        scope.launch(dispatchers.io) {
            messagingRepository.deleteChatMessages(chat)
        }
    }

    fun updateDBContacts() {
        viewModelScope.launch {
            messagingRepository.updateDBContacts()
        }
    }

    suspend fun updateContactsFromPhoneContacts() {
        messagingRepository.updateContactsFromPhoneContacts()
    }

    fun logout(activity: Activity, intent: Intent) {
        viewModelScope.launch {
            val manager = activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            authRepository.logout(manager)
            activity.apply {
                startActivity(intent)
                finish()
            }
        }
    }

    fun updateToken() {
        viewModelScope.launch(dispatchers.io) {
            authRepository.updateToken()
        }
    }

}