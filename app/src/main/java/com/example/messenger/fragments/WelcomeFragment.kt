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

import com.example.messenger.R
import com.example.messenger.databinding.FragmentWelcomeBinding
import com.example.messenger.utils.PermissionHelper

class WelcomeFragment : Fragment() {

    private var binding: FragmentWelcomeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initNextButton()
    }

    /**
     * Initializes the next button to switch to the other fragment.
     */
    private fun initNextButton() {
        binding?.nextButton?.setOnClickListener {
            PermissionHelper.checkAllAppPermissions(requireActivity())
            parentFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_fragment, R.anim.slide_out_fragment)
                replace(this@WelcomeFragment.id, PhoneRegistrationFragment())
                commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}