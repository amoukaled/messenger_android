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

package com.example.messenger.viewModels

import android.app.Activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.messenger.models.AppUser
import com.example.messenger.models.AuthEvent
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.repositories.AuthRepository

import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

class AuthActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val authState: StateFlow<AuthEvent<AppUser>> = authRepository.authState

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credentials: PhoneAuthCredential) {
            loginWithCredentials(credentials)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            authRepository.handleException(e)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            super.onCodeSent(verificationId, token)
            authRepository.token = token
            authRepository.verificationId = verificationId
        }
    }


    fun verifyPhoneNumber(number: String, activity: Activity) {
        authRepository.verifyPhoneNumber(number, activity, callbacks)
    }

    private fun loginWithCredentials(credential: PhoneAuthCredential) {
        viewModelScope.launch(dispatchers.io) {
            authRepository.loginWithCredentials(credential)
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch(dispatchers.io) {
            authRepository.verifyCode(code)
        }
    }

    fun resendVerification(activity: Activity) {
        authRepository.resendVerificationCode(activity, callbacks)
    }

}