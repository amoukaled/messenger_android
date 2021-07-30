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
import android.graphics.BitmapFactory

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage

import kotlinx.coroutines.tasks.await

import java.io.ByteArrayOutputStream

class FirebaseStorage : RemoteStorage {

    private val storage = Firebase.storage
    private val reference = storage.reference
    private val userProfilePicRef = reference.child(RemoteStorage.USER_PROFILE_PIC_STORAGE_PATH)
    private val chatMediaRef = reference.child(RemoteStorage.CHAT_MEDIA_STORAGE_PATH)

    override suspend fun uploadProfilePicture(bitmap: Bitmap, phoneNumber: String): Boolean {
        return uploadImage(bitmap, phoneNumber, userProfilePicRef)
    }

    override suspend fun downloadProfilePicture(phoneNumber: String): Bitmap? {
        return downloadImage(phoneNumber, userProfilePicRef)
    }

    override suspend fun downloadProfilePictures(numbers: List<String>): Map<String, Bitmap?> {
        val pics = mutableMapOf<String, Bitmap?>()

        for (num in numbers) {
            val image = this.downloadProfilePicture(num)
            pics[num] = image
        }

        return pics
    }

    override suspend fun deleteProfilePic(phoneNumber: String): Boolean {
        return try {
            userProfilePicRef.child(phoneNumber).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun uploadChatMedia(bitmap: Bitmap, imageTitle: String): Boolean {
        return uploadImage(bitmap, imageTitle, chatMediaRef)
    }

    override suspend fun downloadChatMedia(imageTitle: String): Bitmap? {
        return downloadImage(imageTitle, chatMediaRef)
    }


    private suspend fun uploadImage(
        bitmap: Bitmap, imageTitle: String, dirRef: StorageReference
    ): Boolean {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val data = stream.toByteArray()
            val userRef = dirRef.child(imageTitle)

            val res = userRef.putBytes(data).await()

            return res.task.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun downloadImage(imageTitle: String, dirRef: StorageReference): Bitmap? {
        return try {
            val picRef = dirRef.child(imageTitle)

            val res = picRef.getBytes(RemoteStorage.IMAGE_SIZE_LIMIT).await()

            return if (res != null) {
                val options = BitmapFactory.Options().apply { inMutable = true }
                val bitmap = BitmapFactory.decodeByteArray(res, 0, res.size, options)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }


}