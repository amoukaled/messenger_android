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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope

import com.example.messenger.R
import com.example.messenger.databinding.FragmentChatParticipantInfoBinding
import com.example.messenger.models.ChatEvent
import com.example.messenger.utils.Constants
import com.example.messenger.viewModels.ChatActivityViewModel

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import kotlinx.coroutines.flow.collect

class ChatParticipantInfoFragment(private val model: ChatActivityViewModel) :
    BottomSheetDialogFragment() {

    private var binding: FragmentChatParticipantInfoBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatParticipantInfoBinding.inflate(inflater, container, false)
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBlockButton()
        lifecycleScope.launchWhenCreated {
            collectFlow()
        }
    }

    /**
     * Initializes the block and unblock button depending on the
     * contact block state.
     */
    private fun initBlockButton() {
        binding?.blockOrUnblockButton?.setOnClickListener {
            model.chat.value.data?.contact?.phoneNumber?.let { num ->
                val isBlocked = model.isContactBlocked(num)

                if (isBlocked)
                    model.unblockContact(num)
                else
                    model.blockContact(num)
            }
        }
    }

    private suspend fun collectFlow() {
        model.chat.collect { event ->
            when (event) {
                is ChatEvent.Data -> {
                    event.data?.let { contact ->
                        binding?.apply {
                            if (infoPB.isVisible) infoPB.isGone = true
                            if (chatParticipantIV.isGone) chatParticipantIV.isVisible = true
                            if (numberET.isGone) numberET.isVisible = true
                            if (statusLL.isGone) statusLL.isVisible = true
                            if (blockOrUnblockButton.isGone)
                                blockOrUnblockButton.isVisible = true

                            // Avatar
                            contact.contact.getProfilePicRoundedDrawable(
                                requireContext(),
                                Constants.EXTRA_LARGE_AVATAR_WIDTH,
                                Constants.EXTRA_LARGE_AVATAR_HEIGHT,
                            )
                                .also { drawable ->
                                    chatParticipantIV.setImageDrawable(drawable)
                                }

                            // Status
                            contact.contact.status?.let { status ->
                                participantStatusTV.text = status
                            }

                            // Phone number
                            contact.contact.phoneNumber.let { number ->
                                numberET.text = number
                            }

                            // Block or unblock text on button.
                            model.isContactBlocked(contact.contact.phoneNumber).let {
                                blockOrUnblockButton.text =
                                    if (it) getString(R.string.unblock) else getString(R.string.block)
                            }
                        }
                    }
                }
                is ChatEvent.Loading -> {
                    binding?.apply {
                        if (infoPB.isGone) infoPB.isVisible = true
                        if (chatParticipantIV.isVisible) chatParticipantIV.isGone = true
                        if (numberET.isVisible) numberET.isGone = true
                        if (statusLL.isVisible) statusLL.isGone = true
                        if (blockOrUnblockButton.isVisible) blockOrUnblockButton.isGone = true
                    }
                }
            }
        }
    }

    // Sets the dialog to full width.
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setOnShowListener { dialogInterface ->
            dialogInterface as Dialog
            val width = resources.getDimensionPixelSize(R.dimen.chat_info_dialog_width)
            val height = resources.getDimensionPixelSize(R.dimen.chat_info_dialog_height)
            dialogInterface.window?.setLayout(width, height)

            val parentLayout =
                dialogInterface.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            parentLayout?.let { pl ->
                val behaviour = BottomSheetBehavior.from(pl)
                setupFullHeight(pl)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    private fun setupFullHeight(dialog: View) {
        val layoutParams = dialog.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.layoutParams = layoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}