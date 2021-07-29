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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat

import com.example.messenger.R

import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Helper class for checking permissions.
 */
object PermissionHelper {

    // The permissions the app needs.
    private val permissions =
        arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )

    /**
     * Checks contacts permission and proceeds accordingly.
     * @param activity The current activity.
     * @param resultLauncher The launcher that is responsible for triggering the callbacks
     * if the permission is granted or not.
     * @param callback The callback that is going to be triggered after granting the permission.
     */
    fun checkContactsPermission(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<String>?,
        callback: (() -> Unit)?
    ) {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                callback?.invoke()
            }
            shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS) -> {
                MaterialAlertDialogBuilder(activity).setTitle(
                    activity.getString(
                        R.string.permission_edu_ui
                    )
                ).setMessage(R.string.contact_permission_message)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
            }
            else -> {
                resultLauncher?.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    /**
     * Checks storage permission and proceeds accordingly.
     * @param activity The current activity.
     * @param resultLauncher The launcher that is responsible for triggering the callbacks
     * if the permission is granted or not.
     * @param callback The callback that is going to be triggered after granting the permission.
     */
    fun checkStoragePermission(
        activity: Activity,
        resultLauncher: ActivityResultLauncher<String>?,
        callback: (() -> Unit)?
    ) {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                callback?.invoke()
            }
            shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {
                MaterialAlertDialogBuilder(activity).setTitle(
                    activity.getString(
                        R.string.permission_edu_ui
                    )
                ).setMessage(R.string.storage_permission_message)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()
            }
            else -> {
                resultLauncher?.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Checks the permissions that are not granted and
     * prompt the user.
     */
    fun checkAllAppPermissions(activity: Activity) {
        val permToBeRequested = mutableListOf<String>()
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permToBeRequested.add(perm)
            }
        }
        if (permToBeRequested.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permToBeRequested.toTypedArray(),
                1
            )
        }
    }
}