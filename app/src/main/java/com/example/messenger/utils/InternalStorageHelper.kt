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

package com.example.messenger.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper class with static methods for internal storage tasks.
 */
object InternalStorageHelper {

    const val PROFILE_PIC_DIR = "profilePictures"
    const val CHAT_MEDIA_DIR = "chatMedia"
    const val CHAT_MEDIA_PREVIEW_DIR = "chatMediaPreview"

    /**
     * Updates local images in storage.
     */
    fun updateLocalProfilePicStorage(context: Context, images: Map<String, Bitmap?>) {
        for (entry in images) {
            val key = entry.key
            val value = entry.value

            updateProfilePicInAppStorage(value, key, context)
        }
    }

    /**
     * If the bitmap is null, it deletes the local image if it exists corresponding to the
     * phone number, else it updates.
     */
    fun updateProfilePicInAppStorage(bitmap: Bitmap?, phoneNumber: String, context: Context) {
        if (bitmap != null) {
            saveImageToAppStorage(bitmap, phoneNumber, context, PROFILE_PIC_DIR)
        } else {
            deleteImageFromAppStorage(context, phoneNumber, PROFILE_PIC_DIR)
        }
    }

    /**
     * Saves the image in the storage.
     * @return Whether the task is successful or not.
     */
    fun saveImageToAppStorage(
        bitmap: Bitmap, imageTitle: String,
        context: Context, dirname: String
    ): Boolean {
        return try {
            val contextWrapper = ContextWrapper(context)

            val directory = contextWrapper.getDir(dirname, Context.MODE_PRIVATE)

            val imageFile = File(directory, imageTitle)

            val fos = FileOutputStream(imageFile)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)

            fos.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Loads the image from storage and returns it as a bitmap. If an image with the corresponding
     * phone number does not exist, it returns null instead.
     */
    fun loadImageFromAppStorage(context: Context, imageTitle: String, dirname: String): Bitmap? {
        return try {
            val contextWrapper = ContextWrapper(context)

            val directory = contextWrapper.getDir(dirname, Context.MODE_PRIVATE)

            val imageFile = File(directory, imageTitle)

            val image = BitmapFactory.decodeStream(FileInputStream(imageFile))

            image
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes the image from storage.
     */
    fun deleteImageFromAppStorage(context: Context, imageTitle: String, dirname: String) {
        val contextWrapper = ContextWrapper(context)

        val directory = contextWrapper.getDir(dirname, Context.MODE_PRIVATE)

        val imageFile = File(directory, imageTitle)
        imageFile.delete()
    }

    /**
     * Deletes the entire storage folder.
     * Used on sign out.
     */
    fun deleteImageStorage(context: Context, dirname: String) {
        try {
            val contextWrapper = ContextWrapper(context)

            contextWrapper.getDir(dirname, Context.MODE_PRIVATE).deleteRecursively()
        } catch (e: Exception) {

        }
    }

}