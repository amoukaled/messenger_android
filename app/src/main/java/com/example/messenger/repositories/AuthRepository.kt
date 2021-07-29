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

package com.example.messenger.repositories

import android.app.Activity
import android.app.ActivityManager

import com.example.messenger.models.AppUser
import com.example.messenger.models.AuthEvent

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    var verificationId: String?
    var token: PhoneAuthProvider.ForceResendingToken?
    val authState: StateFlow<AuthEvent<AppUser>>

    interface Static {
        fun getCurrentUser(): AppUser?
    }

    /**
     * Gets the current [AppUser?].
     */
    fun getCurrentUser(): AppUser?

    /**
     * Verifies the phone number.
     * @param number The phone number to verify.
     * @param activity Current activity.
     * @param cb Callbacks to be triggered on verification events.
     */
    fun verifyPhoneNumber(
        number: String, activity: Activity,
        cb: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )


    /**
     * Logs the user in with credentials.
     */
    suspend fun loginWithCredentials(credentials: PhoneAuthCredential)

    /**
     * Verifies the code that is given by the user.
     * @param code The code given by the user.
     */
    suspend fun verifyCode(code: String)

    /**
     * Resends the verification code to the user.
     * @param activity The Activity the method is going to be called from.
     * @param cb Callbacks to be triggered on verification events.
     */
    fun resendVerificationCode(
        activity: Activity,
        cb: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )

    /**
     * Handles the [e].
     */
    fun handleException(e: Exception)

    /**
     * Handles the auth result.
     * @param result The [AuthResult]
     */
    suspend fun handleAuthResult(result: AuthResult?)

    /**
     * Logs out the user, clears the local database, and sets the token in the remote
     * database to null.
     */
    suspend fun logout(activityManager: ActivityManager)

    /**
     * Gets the block list from the remote database and saves it locally.
     */
    suspend fun getBlockListAndSave()

    /**
     * Gets the user status from the remote database.
     */
    suspend fun getUserStatus()

    /**
     * Updates token of the user.
     * Fired on app launch to make sure the token is up to date.
     * Reason: In case the [com.example.messenger.services.MessagingService] onTokenChanged
     * doesn't update remotely due to connection issues.
     */
    suspend fun updateToken()

}