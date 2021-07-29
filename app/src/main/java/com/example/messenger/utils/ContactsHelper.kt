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

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract

import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.local.entities.Contact
import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.models.LocalContact

/**
 * Helper class for local and remote contact tasks.
 */
class ContactsHelper(
    private val context: Context,
    private val remoteDao: RemoteDao,
    private val contactDao: ContactDao,
    private val remoteStorage: RemoteStorage
) {


    /**
     * Gets the local contacts from the device.
     * @return [Map] of phone number to [LocalContact].
     * @throws Exception If contact permission is denied.
     */
    private fun getLocalContacts(): Map<String, LocalContact> {
        val contacts = mutableMapOf<String, LocalContact>()
        context.contentResolver?.let { cr ->
            val curr = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
            curr?.let { cur ->
                if (cur.count > 0) {
                    while (cur.moveToNext()) {
                        val id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))
                        val name =
                            cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))

                        if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                            val pCur: Cursor? = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            while (pCur?.moveToNext() == true) {
                                val phoneNo: String? = pCur.getString(
                                    pCur.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER
                                    )
                                ).replace("\\s".toRegex(), "").replace("(", "").replace(")", "")
                                    .replace("-", "").run {
                                        // Case 1 +889 number with country code
                                        // Case 2 00999 number with country code but with double zeros
                                        // Case 3 8145464 Number plain number with no country code
                                        // Case 4 03468454864 number starts with zero

                                        val countryCode =
                                            PhoneNumberUtil.getCountryCodeFromDevice(context)

                                        return@run if (get(0) == '0') {
                                            if (get(1) == '0') { // case 2
                                                replaceRange(0, 2, "+")
                                            } else { // case 4
                                                countryCode?.let { nonNullCode ->
                                                    replaceRange(0, 1, "+$nonNullCode")
                                                }

                                            }
                                        } else if (get(0) == '+') { // case 1
                                            this
                                        } else { // case 3
                                            countryCode?.let { nonNullCode ->
                                                "+$nonNullCode$this"
                                            }
                                        }
                                    }

                                phoneNo?.let { nonNullNumber ->
                                    val value = contacts[nonNullNumber]
                                    if (value == null) {
                                        contacts[nonNullNumber] = LocalContact(name, nonNullNumber)
                                    }
                                }
                            }
                            pCur?.close()
                        }
                    }
                }
            }
            curr?.close()
        }

        return contacts
    }

    /**
     * Gets the contact from the remote database and updates the app [Contact] in
     * the table.
     * Updates the local image as well.
     * @param number The number of the contact; primary key in the remote database.
     * @param name The name of the contact.
     */
    suspend fun updateContact(number: String, name: String?) {
        val contact = remoteDao.getAvailableContactByNumber(number)

        contact.data?.let {
            contactDao.insertContact(Contact(it.phoneNumber, name, it.token, it.status))
            val image = remoteStorage.downloadProfilePicture(number)
            InternalStorageHelper.updateProfilePicInAppStorage(image, number, context)
        }
    }

    /**
     * Gets all the contacts that are associated with the local contacts' phone numbers from the remote DB.
     * @return [List] of [Contact].
     */
    private suspend fun getRemoteContacts(): List<Contact> {
        val appContacts = mutableListOf<Contact>()

        val localContacts = this.getLocalContacts()

        localContacts.keys.let { phoneNumbers ->
            val res = remoteDao.getAvailableContactsByNumber(phoneNumbers.toList())

            res.data?.let { fContacts ->
                fContacts.mapNotNull { contact ->
                    localContacts[contact.phoneNumber]?.name?.let { name ->
                        Contact(
                            contact.phoneNumber,
                            name,
                            contact.token,
                            contact.status
                        )
                    }
                }.let { contList ->
                    appContacts.addAll(contList)
                }
            }
        }
        return appContacts
    }

    /**
     * Updates the [Contact] local table by retrieving all local contacts from the phone
     * and checking for remote instances in the remote DB.
     * This is used after the user is authenticated to retrieve the app contacts and when the user refreshes
     * manually the contacts list.
     * Note: Due to firebase limitations on queryIn(), this results in a significant increase
     * in api calls, since we are querying the entire contacts list locally.
     */
    suspend fun updateContactsFromPhoneContacts() {
        val contacts = this.getRemoteContacts()

        contacts.map {
            it.phoneNumber
        }.let {
            val pics = remoteStorage.downloadProfilePictures(it)
            InternalStorageHelper.updateLocalProfilePicStorage(context, pics)
        }

        for (contact in contacts) {
            contactDao.insertContact(contact)
        }
    }

    /**
     * Updates the profile pic locally.
     */
    suspend fun updateUserProfilePic(number: String) {
        val res = remoteStorage.downloadProfilePicture(number)
        InternalStorageHelper.updateProfilePicInAppStorage(res, number, context)
    }

    /**
     * Updates the [Contact] DB entries.
     * Iterates over all the Contacts in the contacts table
     * and updates them.
     * This is used when the application launches which means it is best for reducing the api calls
     * since Firebase Firestore limits the query values to 10.
     */
    suspend fun updateDBContacts() {
        // Getting the database contacts
        val appContacts = contactDao.getAllContacts()

        // Getting the local contacts to update contact names if changed
        val localContacts = getLocalContacts()

        val contactsNum = appContacts.map { it.phoneNumber }

        val res = remoteDao.getAvailableContactsByNumber(contactsNum)

        val filteredContacts = res.data?.let { fsContacts ->
            fsContacts.map { fsContact ->
                val name = localContacts[fsContact.phoneNumber]?.name
                Contact(
                    fsContact.phoneNumber,
                    name,
                    fsContact.token,
                    fsContact.status
                )
            }
        }

        if (filteredContacts != null) {
            for (contact in filteredContacts) {
                contactDao.insertContact(contact)
            }
        }
    }
}