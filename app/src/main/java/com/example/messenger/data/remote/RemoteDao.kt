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

package com.example.messenger.data.remote

import com.example.messenger.data.local.entities.BlockedContact
import com.example.messenger.models.RemoteDatabaseEvent
import com.example.messenger.models.FirestoreContact
import com.example.messenger.models.UserData

import com.google.firebase.firestore.DocumentReference

interface RemoteDao {

    companion object {
        /**
         * The id of the users document.
         */
        const val USER_DOC_ID = "users"

        /**
         * The id of the block lists documents.
         */
        const val BLOCK_LIST_DOC_ID = "blockLists"
    }

    /**
     * Interface for the [RemoteDao] static methods.
     */
    interface Initializer {
        /**
         * Initializes the user in the remote database.
         * @param phoneNumber The phone number and the document id of the user.
         * @param token The Firebase Cloud Messaging token generated on app init.
         */
        suspend fun initUser(
            phoneNumber: String, token: String?
        ): RemoteDatabaseEvent<UserData>
    }

    /**
     * Sets the [UserData] on the remote database.
     * @return [RemoteDatabaseEvent] containing the status of the callback.
     */
    suspend fun updateUserData(userData: UserData): RemoteDatabaseEvent<Unit>

    /**
     * Gets the [UserData] from the remote database.
     * @return [RemoteDatabaseEvent] containing the [UserData].
     */
    suspend fun getUserData(): RemoteDatabaseEvent<UserData>

    /**
     * @param numbers The phone numbers to retrieve from the remote database.
     * @return A list of [FirestoreContact] associated with the [numbers] as a [RemoteDatabaseEvent].
     */
    suspend fun getAvailableContactsByNumber(numbers: List<String>): RemoteDatabaseEvent<List<FirestoreContact>>

    /**
     * @param number The phone number and primary key to check for in the remote database.
     * @return [FirestoreContact] associated with the document id [number] as a [RemoteDatabaseEvent].
     */
    suspend fun getAvailableContactByNumber(number: String): RemoteDatabaseEvent<FirestoreContact>

    /**
     * Updates the user token on the remote database.
     * @param newToken The token to be updated.
     */
    suspend fun updateUserToken(newToken: String?): RemoteDatabaseEvent<Unit>

    /**
     * @return The token of the given phone number. If it doesn't exist, returns null.
     * @param number The phone number to check for.
     */
    suspend fun getTokenFromPhoneNumber(number: String): String?

    /**
     * Updates the block list.
     * @param blockList Block list values to be updated on the remote database.
     */
    suspend fun updateBlockList(blockList: List<BlockedContact>): Boolean

    /**
     * @return The blockList of the implicit app user's phone number.
     */
    suspend fun getBlockList(): RemoteDatabaseEvent<List<BlockedContact>>

    /**
     * Adds a new contact to the block list.
     * @return Whether it was successful or not.
     */
    suspend fun addBlockedContact(phoneNumber: String): Boolean

    /**
     * Removes The blocked contact from the remote database.
     * @return Whether it was successful or not.
     */
    suspend fun removeBlockedContact(phoneNumber: String): Boolean

    // This is optional and for firebase implementation.
    /**
     * Gets the document reference of a specific phone number.
     */
    fun getSpecificUserDocumentReference(phoneNumber: String): DocumentReference
}