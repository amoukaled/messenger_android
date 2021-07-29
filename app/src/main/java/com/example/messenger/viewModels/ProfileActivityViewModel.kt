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

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.messenger.models.DispatcherProvider
import com.example.messenger.models.UserDataEvent
import com.example.messenger.repositories.UserDataRepository

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

class ProfileActivityViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val userPic: StateFlow<UserDataEvent<Bitmap>> = userDataRepository.userPic
    val status = userDataRepository.status

    fun updateUserProfilePic(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(dispatchers.io) {
            kotlin.runCatching {
                contentResolver.openInputStream(uri)?.let { stream ->
                    userDataRepository.updateUserProfilePic(stream)
                }
            }
        }
    }

    fun deleteUserProfilePic() {
        viewModelScope.launch(dispatchers.io) {
            userDataRepository.deleteUserProfilePic()
        }
    }

    fun updateUserStatus(status: String) {
        viewModelScope.launch {
            userDataRepository.updateUserStatus(status)
        }
    }

}