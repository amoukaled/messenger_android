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

import android.os.Bundle
import android.view.inputmethod.EditorInfo

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.example.messenger.databinding.ActivityProfileBinding
import com.example.messenger.fragments.UserDisplayPictureFragment
import com.example.messenger.models.UserDataEvent
import com.example.messenger.repositories.DefaultAuthRepository
import com.example.messenger.utils.Constants
import com.example.messenger.utils.getRoundedDrawableFromBitmap
import com.example.messenger.utils.hideKeyboard
import com.example.messenger.viewModels.ProfileActivityViewModel

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.flow.collect

import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val model: ProfileActivityViewModel by viewModels {
            viewModelFactory
        }
        initBackButton()
        setNumberET()
        initAvatar(model)
        initStatus(model)
    }

    /**
     * Initializes the status of the user, listens for changes,
     * and sets a callback to update status on the remote db on submit.
     */
    private fun initStatus(model: ProfileActivityViewModel) {
        lifecycleScope.launchWhenCreated {
            model.status.collect { event ->
                when (event) {
                    is UserDataEvent.Loading -> {
                        binding.apply {
                            if (statusET.isVisible) statusET.isInvisible = true
                            if (statusPB.isInvisible) statusPB.isVisible = true
                        }
                    }
                    else -> {
                        binding.apply {
                            if (statusET.isInvisible) statusET.isVisible = true
                            if (statusPB.isVisible) statusPB.isInvisible = true
                            statusET.setText(event.data)
                        }
                    }
                }
            }
        }

        binding.statusET.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                hideKeyboard(this)
                // TODO limit the character input
                v.text?.let {
                    val text = it.toString()

                    if (text.isNotBlank()) {
                        model.updateUserStatus(text)
                    }
                }
            }
            return@setOnEditorActionListener true
        }
    }

    /**
     * Initializes the back button
     */
    private fun initBackButton() {
        binding.backToMainButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Initializes the number Edit text to display the
     * user's phone number.
     */
    private fun setNumberET() {
        DefaultAuthRepository.getCurrentUser()?.phoneNumber?.let {
            binding.numberET.text = it
        }
    }

    /**
     * Initializes the avatar, listens to changes, and
     * sets a callback to launch [UserDisplayPictureFragment] to edit
     * the profile pic.
     */
    private fun initAvatar(model: ProfileActivityViewModel) {
        lifecycleScope.launchWhenCreated {
            model.userPic.collect { event ->
                when (event) {
                    is UserDataEvent.Loading -> {
                        binding.apply {
                            if (avatarPB.isGone) avatarPB.isVisible = true
                            if (userProfilePicIV.isVisible) userProfilePicIV.isInvisible = true
                        }
                    }
                    else -> {
                        binding.apply {
                            if (avatarPB.isVisible) avatarPB.isGone = true
                            if (userProfilePicIV.isInvisible) userProfilePicIV.isVisible = true
                            userProfilePicIV.setImageDrawable(
                                getRoundedDrawableFromBitmap(
                                    event.data,
                                    this@ProfileActivity,
                                    Constants.LARGE_AVATAR_WIDTH,
                                    Constants.LARGE_AVATAR_HEIGHT,
                                )
                            )
                        }
                    }
                }
            }
        }

        binding.userProfilePicIV.setOnClickListener {
            UserDisplayPictureFragment().apply {
                show(supportFragmentManager, "userDisplayPic")
            }
        }
    }

}