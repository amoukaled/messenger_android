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
import android.widget.ArrayAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import com.example.messenger.R
import com.example.messenger.databinding.FragmentPhoneRegistrationBinding
import com.example.messenger.models.AuthEvent
import com.example.messenger.utils.PhoneNumberUtil
import com.example.messenger.utils.hideKeyboard
import com.example.messenger.viewModels.AuthActivityViewModel

import com.google.android.material.snackbar.Snackbar

class PhoneRegistrationFragment : Fragment() {

    private var binding: FragmentPhoneRegistrationBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhoneRegistrationBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSpinner()
        val model: AuthActivityViewModel by activityViewModels()
        displayErrorUI(model)
        initSubmitNumberButton(model)
    }

    /**
     * Initializes the submit phone number button.
     */
    private fun initSubmitNumberButton(model: AuthActivityViewModel) {
        binding?.apply {
            submitNumberButton.setOnClickListener {
                countryCodeSpinner.selectedItem?.let { item ->
                    val code = item.toString()
                    val number = phoneNumberET.text

                    if (number == null || number.isBlank()) {
                        val message = getString(R.string.invalid_number)
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).apply {
                            show()
                        }
                    } else {
                        hideKeyboard(requireActivity())
                        model.verifyPhoneNumber(code + number.toString(), requireActivity())
                    }
                }
            }
        }
    }

    /**
     * Displays a SnackBat in case of an error.
     */
    private fun displayErrorUI(model: AuthActivityViewModel) {
        model.authState.value.let {
            if (it is AuthEvent.NumberError || it is AuthEvent.GeneralError) {
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

    /**
     * Initializes the spinner.
     */
    private fun initSpinner() {
        val phoneCodes = PhoneNumberUtil.getAllCountryCodes().map { "+$it" }
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.support_simple_spinner_dropdown_item,
            phoneCodes
        )
        binding?.countryCodeSpinner?.adapter = adapter
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}