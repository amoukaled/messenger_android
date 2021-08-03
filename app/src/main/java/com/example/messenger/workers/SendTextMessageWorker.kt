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

package com.example.messenger.workers

import android.content.Context

import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.models.NotificationData
import com.example.messenger.models.PushNotification
import com.example.messenger.repositories.AuthRepository
import com.example.messenger.repositories.MessagingRepository
import com.example.messenger.utils.Constants
import com.example.messenger.workers.WorkerDataConstants.MESSAGE_ID_KEY

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import javax.inject.Inject

@HiltWorker
class SendTextMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workParams: WorkerParameters
) :
    CoroutineWorker(context, workParams) {

    @Inject
    lateinit var contactDao: ContactDao

    @Inject
    lateinit var messagingRepository: MessagingRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override suspend fun doWork(): Result {
        inputData.apply {
            getLong(MESSAGE_ID_KEY, -1).let { messageId ->
                contactDao.getMessageById(messageId)?.let { message ->
                    message.contactOwner.let { phoneNumber ->
                        contactDao.getContactByPhoneNumber(phoneNumber)?.token?.let { token ->
                            authRepository.getCurrentUser()?.phoneNumber?.let { userNum ->
                                val notification =
                                    PushNotification(
                                        NotificationData(
                                            userNum,
                                            message.data,
                                            Constants.TEXT_MESSAGE,
                                            null,
                                            null,
                                            null,
                                            null
                                        ), token
                                    )
                                messagingRepository.sendTextMessage(
                                    notification,
                                    message,
                                    phoneNumber,
                                )
                                return Result.success()
                            }
                        }
                    }
                }
            }
        }
        return Result.failure()
    }
}