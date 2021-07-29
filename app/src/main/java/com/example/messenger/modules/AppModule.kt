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

package com.example.messenger.modules

import android.content.Context
import androidx.room.Room

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Singleton

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit

import com.example.messenger.api.MessagingApi
import com.example.messenger.models.DispatcherProvider
import com.example.messenger.data.local.dao.ContactDao
import com.example.messenger.data.local.database.AppDatabase
import com.example.messenger.data.remote.FirebaseStorage
import com.example.messenger.data.remote.FirestoreDao
import com.example.messenger.data.remote.RemoteDao
import com.example.messenger.data.remote.RemoteStorage
import com.example.messenger.data.sharedPreferences.UserDataLocalStore
import com.example.messenger.data.sharedPreferences.UserDataPreferences
import com.example.messenger.repositories.*
import com.example.messenger.utils.Constants.BASE_URL
import com.example.messenger.utils.ContactsHelper


@Module(includes = [ViewModelModule::class])
@InstallIn(SingletonComponent::class)
object AppModule {


    @Singleton
    @Provides
    fun providesRetrofitInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun providesNotificationApi(retrofit: Retrofit): MessagingApi {
        return retrofit.create(MessagingApi::class.java)
    }


    @Provides
    @Singleton
    fun providesDispatchers() = object : DispatcherProvider {
        override val main: CoroutineDispatcher
            get() = Dispatchers.Main
        override val io: CoroutineDispatcher
            get() = Dispatchers.IO
        override val default: CoroutineDispatcher
            get() = Dispatchers.Default
        override val unconfined: CoroutineDispatcher
            get() = Dispatchers.Unconfined
    }

    @Singleton
    @Provides
    fun providesAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "AppDB").build()

    @Singleton
    @Provides
    fun providesContactDao(db: AppDatabase): ContactDao = db.contactDao()

    @Provides
    @Singleton
    fun providesUserDataSharedPref(@ApplicationContext context: Context): UserDataLocalStore {
        return UserDataPreferences(context)
    }


    @Provides
    @Singleton
    fun providesFirestoreDao(@ApplicationContext context: Context): RemoteDao {
        return FirestoreDao(context)
    }


    @Singleton
    @Provides
    fun providesFirebaseStorage(): RemoteStorage {
        return FirebaseStorage()
    }

    @Singleton
    @Provides
    fun providesProfilePicRepo(
        @ApplicationContext context: Context, remoteStorage: RemoteStorage,
        userDataLocalStore: UserDataLocalStore, remoteDao: RemoteDao
    ): UserDataRepository {
        return DefaultUserDataRepository(context, remoteStorage, userDataLocalStore, remoteDao)
    }

    @Singleton
    @Provides
    fun providesContactsHelper(
        @ApplicationContext context: Context, remoteDao: RemoteDao,
        contactDao: ContactDao, remoteStorage: RemoteStorage
    ): ContactsHelper {
        return ContactsHelper(context, remoteDao, contactDao, remoteStorage)
    }

    @Provides
    @Singleton
    fun providesDefaultMessagingRepo(
        api: MessagingApi,
        contactDao: ContactDao,
        remoteDao: RemoteDao,
        contactsHelper: ContactsHelper,
        remoteStorage: RemoteStorage
    ): MessagingRepository {
        return DefaultMessagingRepository(api, contactDao, remoteDao, contactsHelper, remoteStorage)
    }


    @Provides
    @Singleton
    fun providesDefaultAuthRepo(
        @ApplicationContext context: Context, userDataLocalStore: UserDataLocalStore,
        contactsHelper: ContactsHelper, remoteDao: RemoteDao, contactDao: ContactDao
    ): AuthRepository {
        return DefaultAuthRepository(
            context, userDataLocalStore, contactsHelper,
            remoteDao, contactDao
        )
    }

}