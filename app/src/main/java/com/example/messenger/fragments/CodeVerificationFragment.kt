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

package com.example.messenger.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import com.example.messenger.R
import com.example.messenger.databinding.FragmentCodeVerificationBinding
import com.example.messenger.models.AuthEvent
import com.example.messenger.utils.hideKeyboard
import com.example.messenger.viewModels.AuthActivityViewModel
import com.google.android.material.snackbar.Snackbar

class CodeVerificationFragment : Fragment() {

    private var binding: FragmentCodeVerificationBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCodeVerificationBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model: AuthActivityViewModel by activityViewModels()
        initErrorUI(model)
        initSubmitCodeButton(model)
        initResendButton(model)
    }

    /**
     * Initializes code resend button.
     */
    private fun initResendButton(model: AuthActivityViewModel) {
        binding?.resendCodeButton?.setOnClickListener {
            hideKeyboard(requireActivity())
            model.resendVerification(requireActivity())
        }
    }

    /**
     * Initializes the submit button.
     */
    private fun initSubmitCodeButton(model: AuthActivityViewModel) {
        binding?.apply {
            submitCodeButton.setOnClickListener {
                val code = codeET.text

                if (code == null || code.isBlank()) {
                    val message = getString(R.string.enter_code)
                    Snackbar.make(root, message, Snackbar.LENGTH_LONG).apply {
                        show()
                    }
                } else {
                    hideKeyboard(requireActivity())
                    model.verifyCode(code.toString())
                }
            }
        }
    }

    /**
     * Displays a SnackBar in case there is an error.
     */
    private fun initErrorUI(model: AuthActivityViewModel) {
        model.authState.value.let {
            if (it is AuthEvent.CodeError) {
                binding?.apply {
                    it.message?.let { message ->
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).apply {
                            show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}