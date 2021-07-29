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

package com.example.messenger.data.sharedPreferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Object with controllers for accessing the user data shared pref storage.
 */
class UserDataPreferences(private val context: Context) : UserDataLocalStore {

    /**
     * The id of UserData [SharedPreferences].
     */
    private val prefId = "userDataPref"

    override var token: String?
        get() = getSharedPreferences().getString("token", null)
        set(value) {
            getSharedPreferences().edit {
                putString("token", value)
                apply()
            }
        }

    override var status: String?
        get() = getSharedPreferences().getString("status", null)
        set(value) {
            getSharedPreferences().edit {
                putString("status", value)
                apply()
            }
        }

    /**
     * Gets the [SharedPreferences] with the [prefId].
     */
    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(prefId, Context.MODE_PRIVATE)
    }

}