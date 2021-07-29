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
 * The user data object that is fetched from the remote database.
 */
data class UserData(
    val status: String
) {

    companion object {
        /**
         * Creates a [UserData] instance from the passed [Map].
         * @throws Exception If any fields are null.
         */
        fun fromMap(map: Map<String, Any>): UserData {
            val status = map["status"] as? String ?: throw Exception()

            return UserData(status)
        }
    }

    /**
     * @return The mapped instance of [UserData]
     */
    fun getUserDataMap(): Map<String, String?> = mapOf(
        "status" to status
    )
}