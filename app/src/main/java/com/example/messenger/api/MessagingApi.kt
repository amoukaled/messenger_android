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

package com.example.messenger.api


import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

import com.example.messenger.utils.Constants.CONTENT_TYPE
import com.example.messenger.BuildConfig
import com.example.messenger.models.PushNotification

interface MessagingApi {

    /**
     * Sends a message via Firebase Cloud Messaging.
     * @param [notification] The notification to send to the user.
     */
    @POST("fcm/send")
    @Headers("Authorization: key=${BuildConfig.SERVER_KEY}", "Content-Type:$CONTENT_TYPE")
    suspend fun sendMessage(@Body notification: PushNotification): Response<ResponseBody>

}