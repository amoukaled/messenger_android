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

package com.example.messenger.data.local.entities

import android.content.Context
import android.graphics.Bitmap

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.messenger.R
import com.example.messenger.utils.InternalStorageHelper


/**
 * Contact Room entity.
 * @property phoneNumber The phone number of the [Contact].
 * @property name The name of the [Contact].
 * @property token The Firebase Cloud Message token for the [Contact].
 * @property status The status of the [Contact] in the Firebase Cloud Messaging.
 */
@Entity
data class Contact(
    @PrimaryKey(autoGenerate = false)
    val phoneNumber: String,

    @ColumnInfo(name = "ContactName")
    val name: String?,

    @ColumnInfo(name = "UserToken")
    var token: String?,

    @ColumnInfo(name = "Status")
    var status: String?
) {

    /**
     * @return Bitmap image of the [Contact] if it exists.
     */
    private fun getProfilePicBitmapFromAppStorage(context: Context): Bitmap? {
        return InternalStorageHelper.loadImageFromAppStorage(context, phoneNumber, InternalStorageHelper.PROFILE_PIC_DIR)
    }

    /**
     * @return Rounded drawable of the user's stored image. If the image doesn't exist, a blank drawable will be returned.
     * @param context The context to get the drawables from.
     * @param desHeight The desired height.
     * @param desWidth The desired width.
     */
    fun getProfilePicRoundedDrawable(context: Context, desWidth: Int, desHeight: Int): RoundedBitmapDrawable {
        val bitmap = this.getProfilePicBitmapFromAppStorage(context)
            ?: AppCompatResources.getDrawable(context, R.drawable.blank_avatar)?.toBitmap()

        val finalBitmap = bitmap?.run {
            Bitmap.createScaledBitmap(this, desWidth, desHeight, false)
        }

        return RoundedBitmapDrawableFactory.create(
            context.resources,
            finalBitmap
        ).apply {
            isCircular = true
        }
    }

}
