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

package com.example.messenger.services

import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.sharedPreferences.UserDataLocalStore
import com.example.messenger.repositories.AuthRepository
import com.example.messenger.repositories.MessagingRepository

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

import javax.inject.Inject

@AndroidEntryPoint
class MessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userDataLocalStore: UserDataLocalStore

    @Inject
    lateinit var remoteDao: RemoteDao

    @Inject
    lateinit var messagingRepo: MessagingRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)

        // Local update
        userDataLocalStore.token = newToken

        // Remote update
        authRepository.getCurrentUser()?.let {
            scope.launch {
                remoteDao.updateUserToken(newToken)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        authRepository.getCurrentUser()?.let {
            scope.launch {
                messagingRepo.receiveRemoteMessage(applicationContext, message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

}