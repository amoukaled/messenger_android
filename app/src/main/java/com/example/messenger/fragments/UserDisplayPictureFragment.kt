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
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope

import com.example.messenger.R
import com.example.messenger.databinding.FragmentUserDisplayPictureBinding
import com.example.messenger.models.UserDataEvent
import com.example.messenger.utils.PermissionHelper
import com.example.messenger.viewModels.ProfileActivityViewModel

import com.google.android.material.bottomsheet.BottomSheetBehavior

import kotlinx.coroutines.flow.collect

class UserDisplayPictureFragment : DialogFragment() {

    private var binding: FragmentUserDisplayPictureBinding? = null
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private lateinit var getContent: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserDisplayPictureBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getContent =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val model: ProfileActivityViewModel by activityViewModels()
                result.data?.data?.let { uri ->
                    model.updateUserProfilePic(uri, requireActivity().contentResolver)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model: ProfileActivityViewModel by activityViewModels()

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    openStorage()
                }
            }

        setProfilePic(model)
        initEditPic()
        initDeleteButton(model)
    }

    /**
     * Initializes the delete pic button.
     */
    private fun initDeleteButton(model: ProfileActivityViewModel) {
        binding?.removePicButton?.setOnClickListener {
            model.deleteUserProfilePic()
        }
    }


    /**
     * Opens the storage to pick the image.
     */
    private fun openStorage() {
        val intent = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        val pIntent =
            PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val sReq = IntentSenderRequest.Builder(pIntent).build()
        getContent.launch(sReq)
    }

    /**
     * Initializes edit pic button.
     */
    private fun initEditPic() {
        binding?.editPicButton?.setOnClickListener {
            PermissionHelper.checkStoragePermission(requireActivity(), requestPermissionLauncher) {
                openStorage()
            }
        }
    }

    /**
     * Sets the profile pic from the flow.
     */
    private fun setProfilePic(model: ProfileActivityViewModel) {
        lifecycleScope.launchWhenCreated {
            model.userPic.collect { event ->
                when (event) {
                    is UserDataEvent.Available -> {
                        binding?.apply {
                            profilePicPB.isGone = true
                            profilePicIV.isVisible = true
                            profilePicIV.setImageBitmap(event.data)
                            profilePicIV.background = null
                        }
                    }
                    is UserDataEvent.Empty -> {
                        binding?.apply {
                            profilePicPB.isGone = true
                            profilePicIV.isVisible = true
                            profilePicIV.apply {
                                setImageDrawable(
                                    ContextCompat.getDrawable(context, R.drawable.ic_person)
                                )
                                background =
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.display_pic_background
                                    )
                                setPadding(100, 100, 100, 100)
                            }
                        }
                    }
                    is UserDataEvent.Loading -> {
                        binding?.apply {
                            profilePicPB.isVisible = true
                            profilePicIV.isGone = true
                        }
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())

        dialog.setOnShowListener { dialogInterface ->
            dialogInterface as Dialog

            val width = resources.getDimensionPixelSize(R.dimen.display_picture_dialog_width)
            val height = resources.getDimensionPixelSize(R.dimen.display_picture_dialog_height)
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
        requestPermissionLauncher = null
    }

}