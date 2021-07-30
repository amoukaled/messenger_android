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

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.example.messenger.R
import com.example.messenger.databinding.ActivityAuthBinding
import com.example.messenger.fragments.CodeVerificationFragment
import com.example.messenger.fragments.LoadingFragment
import com.example.messenger.fragments.PhoneRegistrationFragment
import com.example.messenger.fragments.WelcomeFragment
import com.example.messenger.models.AuthEvent
import com.example.messenger.viewModels.AuthActivityViewModel

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.flow.collect

import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val model: AuthActivityViewModel by viewModels {
            viewModelFactory
        }
        lifecycleScope.launchWhenCreated {
            updateUI(model)
        }
    }

    private suspend fun updateUI(model: AuthActivityViewModel) {
        model.authState.collect { event ->
            when (event) {
                is AuthEvent.Authenticated -> {
                    // Push to MainActivity
                    Intent(this, MainActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(it)
                        finish()
                    }
                }
                is AuthEvent.Loading -> {
                    // Replace with progress bar indicator
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                        replace(binding.authFL.id, LoadingFragment())
                        commit()
                    }
                }
                is AuthEvent.Pending -> {
                    // Replace with Code input fragment
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                        replace(binding.authFL.id, CodeVerificationFragment())
                        commit()
                    }
                }
                is AuthEvent.UnAuthenticated -> {
                    // Replace with Welcome fragment
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                        replace(binding.authFL.id, WelcomeFragment())
                        commit()
                    }
                }
                is AuthEvent.CodeError -> {
                    // Replace with Code input fragment that will display the error message
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                        replace(binding.authFL.id, CodeVerificationFragment())
                        commit()
                    }
                }
                else -> { // AuthEvent.GeneralError or AuthEvent.NumberError
                    // Replace with PhoneNumber input fragment to display the error message
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                        replace(binding.authFL.id, PhoneRegistrationFragment())
                        commit()
                    }
                }
            }
        }
    }
}