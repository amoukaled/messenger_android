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

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap

import com.example.messenger.R

import java.text.DateFormat
import java.util.*

import kotlin.math.floor


// DEBUG
// Log TAG for easier filtering
private const val tag = "DEBUG/"

/**
 * Logs the instance to the console.
 * Used in debugging.
 */
fun logInstance(instance: Any?, payload: String = "") {
    if (payload.isNotBlank()) {
        Log.d(
            "$tag/LoggingInstance",
            "$payload ${instance.toString()}"
        )
    } else {
        Log.d(
            "$tag/LoggingInstance",
            "${instance?.javaClass?.name} is ${instance.toString()}"
        )
    }
}

/**
 * Hides the keyboard.
 */
fun hideKeyboard(activity: Activity) {
    val imm: InputMethodManager =
        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view: View? = activity.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

/**
 * Gets the rounded drawable of the bitmap.
 * If the bitmap is null, it returns a blank avatar.
 */
fun getRoundedDrawableFromBitmap(
    bitmap: Bitmap?, context: Context,
    desWidth: Int, desHeight: Int
): RoundedBitmapDrawable {
    val bitmap1 = bitmap
        ?: AppCompatResources.getDrawable(context, R.drawable.blank_avatar)?.toBitmap()

    val finalBitmap = bitmap1?.run {
        Bitmap.createScaledBitmap(this, desWidth, desHeight, false)
    }

    return RoundedBitmapDrawableFactory.create(
        context.resources,
        finalBitmap
    ).apply {
        isCircular = true
    }
}

/**
 * Formats the date and returns a string based on the
 * time format.
 */
fun formatTimeToString(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
}

/**
 * Generates a random id.
 * @param length The desired length. Default is 30.
 */
fun idGenerator(length: Int = 30): String {
    val characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-(@#*%&_1234567890"
    var id = ""

    for (i in 0 until length) {
        val random = floor((Math.random() * characters.length)).toInt()
        id += characters[random]
    }

    return id
}

fun getRealPathFromUri(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    try {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        cursor = context.contentResolver.query(uri, proj, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        return columnIndex?.let {
            cursor?.getString(it)
        }
    } finally {
        cursor?.close()
    }
}