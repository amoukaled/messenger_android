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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

import java.io.ByteArrayOutputStream

object ImageUtil {

    /**
     * Returns the preview version for the passed bitmap.
     */
    private fun getDisplayPreviewFromBitmap(bitmap: Bitmap): Bitmap {
        var width: Int = bitmap.width
        var height: Int = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 12
            height = (width / bitmapRatio).toInt()
        } else {
            height = 12
            width = (height * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Returns the hex string of the passed bitmap.
     */
    fun getPreviewHexFromBitmap(bitmap: Bitmap): String {
        val minifiedBM = getDisplayPreviewFromBitmap(bitmap)

        val baos = ByteArrayOutputStream()

        minifiedBM.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val bytes = baos.toByteArray()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
     * Returns the bitmap of the passed hex string
     */
    fun getBitmapFromHex(hex: String, width: Int, height: Int): Bitmap {
        val bytes = Base64.decode(hex, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }


}