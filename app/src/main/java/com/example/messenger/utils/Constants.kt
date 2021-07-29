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

object Constants {
    /**
     * The URL of the FCM.
     */
    const val BASE_URL = "https://fcm.googleapis.com"

    /**
     * The content type for the request.
     */
    const val CONTENT_TYPE = "application/json"

    /**
     * The Intent.getExtra map id.
     */
    const val CHAT_ID = "chatId"

    const val EXPAND_IMAGE_KEY = "expandImageId"

    const val IMAGE_DIR_NAME_KEY = "imageDirname"


    // Avatar sizes
    const val SMALL_AVATAR_WIDTH = 35
    const val SMALL_AVATAR_HEIGHT = 35

    const val MEDIUM_AVATAR_WIDTH = 60
    const val MEDIUM_AVATAR_HEIGHT = 60

    const val LARGE_AVATAR_WIDTH = 120
    const val LARGE_AVATAR_HEIGHT = 120

    const val EXTRA_LARGE_AVATAR_WIDTH = 180
    const val EXTRA_LARGE_AVATAR_HEIGHT = 180

    // Message types // TODO check
    const val TEXT_MESSAGE = 1
    const val IMAGE_MESSAGE = 2
    const val VIDEO_MESSAGE = 3
    const val AUDIO_MESSAGE = 4
}