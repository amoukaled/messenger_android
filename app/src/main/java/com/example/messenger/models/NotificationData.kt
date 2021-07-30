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

package com.example.messenger.models

/**
 * Notification data for FCM.
 */
data class NotificationData(
    val title: String, // TODO change title -> From
    val message: String?, // Message body
    val messageType: Int, // Message type (text, image, vn, etc..)
    var image: String?, // The image id
    val imagePreview: String?, // Low resolution image of the sent one to display with blur effect before downloading.
    val imageWidth: Int?, // The width of the image sent. Used to enlarge the imagePreview that helps with achieving the blur effect.
    val imageHeight: Int? // The height of the image sent. Used to enlarge the imagePreview that helps with achieving the blur effect.
)

// Dev note: The image preview and blur effect can be implemented differently and will be revisited in the future.