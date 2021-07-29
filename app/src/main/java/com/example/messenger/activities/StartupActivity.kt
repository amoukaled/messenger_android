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

package com.example.messenger.activities

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import com.example.messenger.models.AuthEvent
import com.example.messenger.repositories.AuthRepository

import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject

/**
 * This activity is an invisible startup activity which determines the appropriate
 * activity to display to the user.
 */
@AndroidEntryPoint
class StartupActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authRepository.authState.value.let { event ->
            when (event) {
                is AuthEvent.Authenticated -> {
                    Intent(this, MainActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                }
                else -> {
                    Intent(this, AuthActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                }
            }
        }
    }
}