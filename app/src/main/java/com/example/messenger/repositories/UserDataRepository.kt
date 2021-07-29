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

import android.graphics.Bitmap
import com.example.messenger.models.UserDataEvent
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream

interface UserDataRepository {

    val userPic: StateFlow<UserDataEvent<Bitmap>>
    val status: StateFlow<UserDataEvent<String>>

    /**
     * Updates the profile pic of the authenticated app user.
     */
    suspend fun updateUserProfilePic(stream: InputStream)

    /**
     * Deletes the pic of the authenticated app user.
     */
    suspend fun deleteUserProfilePic()

    /**
     * Updates the status of the authenticated app user.
     */
    suspend fun updateUserStatus(status: String)

}