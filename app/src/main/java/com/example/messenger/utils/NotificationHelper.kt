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

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap

import com.example.messenger.R
import com.example.messenger.activities.MainActivity

import kotlin.random.Random

/**
 * Helper class for sending notifications.
 */
object NotificationHelper {

    private const val channelId = "not_channel"
    private const val channelName = "New message"

    fun displayNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.image_foreground)
            .setLargeIcon(
                AppCompatResources.getDrawable(context, R.drawable.image_foreground)?.toBitmap()
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(manager: NotificationManager) {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                description = "New message"
            }
        manager.createNotificationChannel(channel)
    }


    /**
     * Checks whether the app is running or not.
     */
    fun isAppRunning(context: Context): Boolean {
        var isAppRunning = false
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).let { am ->
            val runningTasks = am.getRunningTasks(1)[0]
            runningTasks.topActivity?.packageName?.let { packageName ->
                if (packageName == context.packageName) {
                    isAppRunning = true
                }
            }
        }
        return isAppRunning
    }


}