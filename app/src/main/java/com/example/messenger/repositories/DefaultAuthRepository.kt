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
import android.content.Context

import com.example.messenger.R
import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.remote.FirestoreDao
import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.sharedPreferences.UserDataLocalStore
import com.example.messenger.models.AppUser
import com.example.messenger.models.AuthEvent
import com.example.messenger.utils.ContactsHelper
import com.example.messenger.utils.InternalStorageHelper

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

import java.util.concurrent.TimeUnit

class DefaultAuthRepository(
    private val context: Context,
    private val userDataLocalStore: UserDataLocalStore,
    private val contactsHelper: ContactsHelper,
    private val remoteDao: RemoteDao,
    private val contactDao: ContactDao
) :
    AuthRepository {

    // private
    private val auth: FirebaseAuth = Firebase.auth
    private var phoneNum: String? = null
    private val _authState = MutableStateFlow(initAuthEvent())

    // public
    override var verificationId: String? = null
    override var token: PhoneAuthProvider.ForceResendingToken? = null
    override val authState: StateFlow<AuthEvent<AppUser>> = _authState


    companion object : AuthRepository.Static {
        override fun getCurrentUser(): AppUser? {
            Firebase.auth.currentUser?.let { fbUser ->
                fbUser.phoneNumber?.let { num ->
                    fbUser.uid.let { id ->
                        return AppUser(id, num)
                    }
                }
            }
            return null
        }
    }


    override fun getCurrentUser(): AppUser? = appUserFromFirebaseUser(auth.currentUser)

    override fun verifyPhoneNumber(
        number: String,
        activity: Activity,
        cb: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        phoneNum = number
        auth.useAppLanguage()
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(cb)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        _authState.value = AuthEvent.Pending()
    }

    override suspend fun loginWithCredentials(
        credentials: PhoneAuthCredential,
    ) {
        try {
            _authState.value = AuthEvent.Loading()
            val res = auth.signInWithCredential(credentials).await()
            handleAuthResult(res)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override suspend fun verifyCode(code: String) {
        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(it, code)
            loginWithCredentials(credential)
        }
    }

    override fun resendVerificationCode(
        activity: Activity,
        cb: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        phoneNum?.let { num ->
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(num)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(cb)
            token?.let {
                optionsBuilder.setForceResendingToken(it)
            }
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        }

    }

    override fun handleException(e: Exception) {
        when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                val message = context.getString(R.string.invalid_code)
                _authState.value = AuthEvent.CodeError(message)
            }
            is FirebaseTooManyRequestsException -> {
                val message = context.getString(R.string.exceeded_quota)
                _authState.value = AuthEvent.NumberError(message)
            }
            is FirebaseNetworkException -> {
                val message = context.getString(R.string.network_error)
                _authState.value = AuthEvent.GeneralError(message)
            }
            else -> {
                val message = context.getString(R.string.went_wrong)
                _authState.value = AuthEvent.GeneralError(message)
            }
        }
    }

    override suspend fun handleAuthResult(
        result: AuthResult?
    ) {
        result?.let { res ->
            res.user?.let { fbUser ->
                appUserFromFirebaseUser(fbUser)?.let { user ->
                    FirestoreDao.initUser(user.phoneNumber, userDataLocalStore.token)
                    contactsCB()
                    getUserStatus()
                    getBlockListAndSave()
                    _authState.value = AuthEvent.Authenticated(user)
                    return
                }
            }
        }

        _authState.value = AuthEvent.UnAuthenticated()
    }

    override suspend fun logout(activityManager: ActivityManager) {
        getCurrentUser()?.let {
            remoteDao.updateUserToken(null)
            contactDao.clearContactTable()
            contactDao.clearMessageTable()
            contactDao.clearBlockedContactTable()
            userDataLocalStore.status = null
            InternalStorageHelper.deleteImageStorage(context, InternalStorageHelper.PROFILE_PIC_DIR)
            auth.signOut()
            _authState.value = initAuthEvent()
            activityManager.clearApplicationUserData() // This will cause the app to shut down, but guarantees a successful wipe
        }
    }

    override suspend fun getBlockListAndSave() {
        val res = remoteDao.getBlockList()
        res.data?.let { list ->
            for (contact in list) {
                contactDao.insertBlockedContact(contact)
            }
        }
    }

    override suspend fun getUserStatus() {
        val data = remoteDao.getUserData()

        data.data?.let {
            userDataLocalStore.status = it.status
        }
    }

    override suspend fun updateToken() {
        val token = userDataLocalStore.token
        remoteDao.updateUserToken(token)
    }

    /**
     * Initializes the Auth StateFlow.
     */
    private fun initAuthEvent(): AuthEvent<AppUser> {
        return getCurrentUser().let {
            return@let if (it == null) {
                AuthEvent.UnAuthenticated()
            } else {
                AuthEvent.Authenticated(it)
            }
        }
    }

    /**
     * Converts [FirebaseUser] to [AppUser].
     */
    private fun appUserFromFirebaseUser(user: FirebaseUser?): AppUser? {
        user?.let { fbUser ->
            fbUser.phoneNumber?.let { num ->
                fbUser.uid.let { id ->
                    return AppUser(id, num)
                }
            }
        }
        return null
    }

    /**
     * The callback to get the contacts from the phone and saves them locally.
     */
    private suspend fun contactsCB() {
        try {
            contactsHelper.updateContactsFromPhoneContacts()
            this.getCurrentUser()?.phoneNumber?.let {
                contactsHelper.updateUserProfilePic(it)
            }
        } catch (e: Exception) {
        }
    }

}
