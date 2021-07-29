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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.messenger.viewModels.AuthActivityViewModel
import com.example.messenger.viewModels.MainActivityViewModel
import com.example.messenger.viewModels.MessengerViewModelFactory
import com.example.messenger.viewModels.ProfileActivityViewModel

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(AuthActivityViewModel::class)
    abstract fun bindAuthActivityViewModel(userViewModel: AuthActivityViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainActivityViewModel(repoViewModel: MainActivityViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileActivityViewModel::class)
    abstract fun bindProfileActivityViewModel(repoViewModel: ProfileActivityViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: MessengerViewModelFactory): ViewModelProvider.Factory
}