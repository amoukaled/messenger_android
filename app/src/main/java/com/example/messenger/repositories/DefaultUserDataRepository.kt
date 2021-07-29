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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.data.sharedPreferences.UserDataLocalStore
import com.example.messenger.models.RemoteDatabaseEvent
import com.example.messenger.models.UserData
import com.example.messenger.models.UserDataEvent
import com.example.messenger.utils.InternalStorageHelper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import java.io.InputStream

class DefaultUserDataRepository(
    private val context: Context,
    private val remoteStorage: RemoteStorage,
    private val userDataLocalStore: UserDataLocalStore,
    private val remoteDao: RemoteDao
) : UserDataRepository {

    // Pic
    private val _userPic = MutableStateFlow(getUserPic())
    override val userPic: StateFlow<UserDataEvent<Bitmap>> = _userPic

    // Status
    private val _status = MutableStateFlow(initStatusFlow())
    override val status: StateFlow<UserDataEvent<String>> = _status


    override suspend fun updateUserStatus(status: String) {
        _status.value = UserDataEvent.Loading()
        when (remoteDao.updateUserData(UserData(status))) {
            is RemoteDatabaseEvent.Failure -> {
                refreshStatus()
            }
            is RemoteDatabaseEvent.RetrieveSuccess -> Unit
            is RemoteDatabaseEvent.WriteSuccess -> {
                userDataLocalStore.status = status
                refreshStatus()
            }
        }
    }


    override suspend fun updateUserProfilePic(stream: InputStream) {
        DefaultAuthRepository.getCurrentUser()?.phoneNumber?.let { num ->
            BitmapFactory.decodeStream(stream)?.let { bitmap ->
                _userPic.value = UserDataEvent.Loading()
                remoteStorage.uploadProfilePicture(bitmap, num)
                InternalStorageHelper.saveImageToAppStorage(
                    bitmap, num,
                    context,
                    InternalStorageHelper.PROFILE_PIC_DIR
                )
                _userPic.value = getUserPic()
            }
        }
    }

    override suspend fun deleteUserProfilePic() {
        DefaultAuthRepository.getCurrentUser()?.phoneNumber?.let { num ->
            _userPic.value = UserDataEvent.Loading()
            val res = remoteStorage.deleteProfilePic(num)
            if (res) {
                InternalStorageHelper.deleteImageFromAppStorage(
                    context, num,
                    InternalStorageHelper.PROFILE_PIC_DIR
                )
                _userPic.value = getUserPic()
            } else {
                _userPic.value = getUserPic()
            }
        }
    }

    /**
     * Initializes the status flow.
     */
    private fun initStatusFlow(): UserDataEvent<String> {
        userDataLocalStore.status.let {
            return if (it == null) {
                UserDataEvent.Empty()
            } else {
                UserDataEvent.Available(it)
            }
        }
    }

    /**
     * Gets the user picture event.
     */
    private fun getUserPic(): UserDataEvent<Bitmap> {
        return DefaultAuthRepository.getCurrentUser()?.phoneNumber?.let {
            InternalStorageHelper.loadImageFromAppStorage(
                context, it,
                InternalStorageHelper.PROFILE_PIC_DIR
            )
        }.let { bm ->
            return@let if (bm == null) {
                UserDataEvent.Empty()
            } else {
                UserDataEvent.Available(bm)
            }
        }
    }

    /**
     * Refreshes the status flow.
     */
    private fun refreshStatus() {
        userDataLocalStore.status.let {
            if (it == null) {
                _status.value = UserDataEvent.Empty()
            } else {
                _status.value = UserDataEvent.Available(it)
            }
        }
    }

}