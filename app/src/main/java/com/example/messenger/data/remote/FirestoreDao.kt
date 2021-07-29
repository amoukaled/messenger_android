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

import android.content.Context

import com.example.messenger.R
import com.example.messenger.data.local.entities.BlockedContact
import com.example.messenger.models.FirestoreContact
import com.example.messenger.models.RemoteDatabaseEvent
import com.example.messenger.models.UserData
import com.example.messenger.repositories.DefaultAuthRepository

import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.tasks.await

class FirestoreDao(private val context: Context) : RemoteDao {

    private val db = Firebase.firestore

    companion object : RemoteDao.Initializer {
        override suspend fun initUser(
            phoneNumber: String, token: String?
        ): RemoteDatabaseEvent<UserData> {
            val usersCollection = Firebase.firestore.collection(RemoteDao.USER_DOC_ID)
            val blockListCollection = Firebase.firestore.collection(RemoteDao.BLOCK_LIST_DOC_ID)
            val data =
                hashMapOf("token" to token, "status" to "Available")
            return try {
                val userDoc = usersCollection.document(phoneNumber).get().await()
                if (!userDoc.exists()) {
                    usersCollection.document(phoneNumber).set(data).await()
                } else {
                    usersCollection.document(phoneNumber).update(mapOf("token" to token)).await()
                }

                val blockList = blockListCollection.document(phoneNumber).get().await()

                if (!blockList.exists()) {
                    blockListCollection.document(phoneNumber)
                        .set(mapOf("blockList" to listOf<String>()))
                }

                RemoteDatabaseEvent.WriteSuccess()
            } catch (e: Exception) {
                RemoteDatabaseEvent.Failure("An error occurred.")
            }
        }
    }

    override suspend fun updateUserData(userData: UserData): RemoteDatabaseEvent<Unit> {
        return try {
            getUserDoc().update(userData.getUserDataMap()).await()
            RemoteDatabaseEvent.WriteSuccess()
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun getUserData(): RemoteDatabaseEvent<UserData> {
        return try {
            val doc = getUserDoc().get().await()
            val data = UserData.fromMap(doc.data ?: throw Exception())

            RemoteDatabaseEvent.RetrieveSuccess(data)
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun getAvailableContactsByNumber(numbers: List<String>): RemoteDatabaseEvent<List<FirestoreContact>> {
        return try {

            // Due to Firebase limitations, using CollectionReference().whereIn
            // is only available with a max of 10 values at a time.

            val queries = mutableListOf<QuerySnapshot>()

            var from = 0
            var to = if (numbers.size < 10) numbers.size else 10

            // Iterates over the numbers and gets the remote values
            while (to <= numbers.size) {
                val sublist = numbers.subList(from, to)
                getByBatch(sublist)?.let {
                    queries.add(it)
                }

                if (to == numbers.size) {
                    break
                }

                from = if ((to + 1) > numbers.size) from else to + 1
                to = if ((to + 11) < numbers.size) to + 11 else numbers.size
            }

            val contactsList = mutableListOf<FirestoreContact>()

            for (query in queries) {
                query.documents.let {
                    for (doc in it) {
                        doc?.data?.let { map ->
                            (map["token"] as String?).let { token ->
                                val status = map["status"] as? String
                                contactsList.add(FirestoreContact(doc.id, token, status))
                            }
                        }
                    }
                }
            }

            RemoteDatabaseEvent.RetrieveSuccess(contactsList)
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun getAvailableContactByNumber(number: String): RemoteDatabaseEvent<FirestoreContact> {
        return try {
            val snapshot = getUsersCollection().document(number).get().await()

            // Taking the snapshot if it exists and returning the FirestoreContact
            snapshot?.takeIf {
                it.exists()
            }?.data?.let { map ->
                (map["token"] as String?).let { token ->
                    val status = map["status"] as? String
                    return RemoteDatabaseEvent.RetrieveSuccess(
                        FirestoreContact(
                            number,
                            token,
                            status
                        )
                    )
                }
            }

            throw Exception()
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun updateUserToken(newToken: String?): RemoteDatabaseEvent<Unit> {
        return try {
            getUserDoc().update(mapOf("token" to newToken)).await()
            RemoteDatabaseEvent.WriteSuccess()
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun getTokenFromPhoneNumber(number: String): String? {
        return try {
            val doc = getUsersCollection().document(number).get().await()
            doc.data?.get("token") as String?
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateBlockList(blockList: List<BlockedContact>): Boolean {
        return try {
            val numbers = blockList.map { it.phoneNumber }
            getUserBlockListDoc().update(mapOf("blockList" to numbers)).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getBlockList(): RemoteDatabaseEvent<List<BlockedContact>> {
        return try {
            val doc = getUserBlockListDoc().get().await()

            val list = doc.data?.get("blockList") as List<*>

            val contacts = list.mapNotNull { BlockedContact(it as String) }

            RemoteDatabaseEvent.RetrieveSuccess(contacts)
        } catch (e: Exception) {
            RemoteDatabaseEvent.Failure(context.getString(R.string.error_occurred))
        }
    }

    override suspend fun addBlockedContact(phoneNumber: String): Boolean {
        return try {
            val map = hashMapOf<String, Any>()

            map["blockList"] = FieldValue.arrayUnion(phoneNumber)

            getUserBlockListDoc().update(map).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeBlockedContact(phoneNumber: String): Boolean {
        return try {
            val map = hashMapOf<String, Any>()

            map["blockList"] = FieldValue.arrayRemove(phoneNumber)

            getUserBlockListDoc().update(map).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getSpecificUserDocumentReference(phoneNumber: String): DocumentReference {
        return getUsersCollection().document(phoneNumber)
    }

    /**
     * Gets the current user phone number.
     * @throws [Exception] If the user is unauthenticated.
     */
    private fun getUserPhoneNumber(): String {
        return DefaultAuthRepository.getCurrentUser()?.phoneNumber ?: throw Exception()
    }

    /**
     * Gets the [CollectionReference] of the users collection.
     * @throws [Exception] If the user is unauthenticated.
     */
    private fun getUsersCollection(): CollectionReference {
        return getUserPhoneNumber().let {
            db.collection(RemoteDao.USER_DOC_ID)
        }
    }

    /**
     * Gets the [CollectionReference] of the users collection.
     * @throws [Exception] If the user is unauthenticated.
     */
    private fun getBlockListsCollection(): CollectionReference {
        return getUserPhoneNumber().let {
            db.collection(RemoteDao.BLOCK_LIST_DOC_ID)
        }
    }

    /**
     * Gets the [DocumentReference] of the user.
     * @throws [Exception] If the user is unauthenticated.
     */
    private fun getUserDoc(): DocumentReference {
        return getUserPhoneNumber().let {
            getUsersCollection().document(it)
        }
    }

    /**
     * Gets the [DocumentReference] of the user.
     * @throws [Exception] If the user is unauthenticated.
     */
    private fun getUserBlockListDoc(): DocumentReference {
        return getUserPhoneNumber().let {
            getBlockListsCollection().document(it)
        }
    }

    /**
     * Gets the snapshots in batches of 10.
     */
    private suspend fun getByBatch(numbers: List<String>): QuerySnapshot? {
        return getUsersCollection().whereIn(FieldPath.documentId(), numbers).get().await()
    }

}