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

package com.example.messenger.data.remote

import android.graphics.Bitmap

interface RemoteStorage {

    companion object {
        const val USER_PROFILE_PIC_STORAGE_PATH = "userProfilePics"
        const val CHAT_MEDIA_STORAGE_PATH = "chatMedia"
        const val IMAGE_SIZE_LIMIT: Long = 1024 * 1024 * 5 // 5 Megabytes
    }

    /**
     * Uploads the file to the remote storage.
     * @param phoneNumber The phone number of the user.
     */
    suspend fun uploadProfilePicture(bitmap: Bitmap, phoneNumber: String): Boolean

    /**
     * Downloads the profile pic of the user if it exists.
     * @return The bitmap image if it exists.
     * @param phoneNumber The phone number of the user.
     */
    suspend fun downloadProfilePicture(phoneNumber: String): Bitmap?

    /**
     * Downloads the profile pics of the given numbers.
     * @return [Map] of the phone numbers and image.
     */
    suspend fun downloadProfilePictures(numbers: List<String>): Map<String, Bitmap?>

    /**
     * Deletes the profile pic of the given number.
     * @return If it was deleted successfully.
     * @param phoneNumber The phone number of the user.
     */
    suspend fun deleteProfilePic(phoneNumber: String): Boolean

    /**
     * Uploads chat media.
     * @param bitmap The image bitmap to upload.
     * @param imageTitle The image title/ID.
     */
    suspend fun uploadChatMedia(bitmap: Bitmap, imageTitle: String): Boolean

    /**
     * Downloads chat media.
     * @param imageTitle The image title/ID to retrieve.
     */
    suspend fun downloadChatMedia(imageTitle: String): Bitmap?

}