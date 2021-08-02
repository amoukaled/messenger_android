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
import com.example.messenger.utils.ImageUtil
import com.example.messenger.utils.InternalStorageHelper
import com.example.messenger.workers.WorkerDataConstants.MESSAGE_ID_KEY

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import javax.inject.Inject

@HiltWorker
class SendImageMessageWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var contactDao: ContactDao

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var messagingRepository: MessagingRepository

    override suspend fun doWork(): Result {
        inputData.apply {
            getString(WorkerDataConstants.PHONE_NUM_KEY)?.let { phoneNumber ->
                contactDao.getContactByPhoneNumber(phoneNumber)?.token?.let { token ->
                    getString(WorkerDataConstants.LOCAL_IMAGE_ID_KEY)?.let { imageId ->
                        authRepository.getCurrentUser()?.phoneNumber?.let { userNumber ->
                            getLong(MESSAGE_ID_KEY, -1).let { messageId ->
                                contactDao.getMessageById(messageId)?.let { message ->
                                    InternalStorageHelper.loadImageFromAppStorage(
                                        applicationContext,
                                        imageId,
                                        InternalStorageHelper.CHAT_MEDIA_DIR
                                    )?.let { bitmap ->
                                        val preview =
                                            ImageUtil.getPreviewHexFromBitmap(bitmap)
                                        val notification =
                                            PushNotification(
                                                NotificationData(
                                                    userNumber, message.data, Constants.IMAGE_MESSAGE,
                                                    null, preview, bitmap.width, bitmap.height
                                                ), token
                                            )
                                        messagingRepository.sendImageMessage(
                                            notification,
                                            message,
                                            imageId,
                                            bitmap,
                                            phoneNumber,
                                            applicationContext
                                        )
                                        return Result.success()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return Result.failure()
    }
}