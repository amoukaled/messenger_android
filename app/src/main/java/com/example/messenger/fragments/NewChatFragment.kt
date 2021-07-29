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
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.messenger.activities.ChatActivity
import com.example.messenger.adapters.ContactItemAdapter
import com.example.messenger.databinding.FragmentNewChatBinding
import com.example.messenger.utils.Constants
import com.example.messenger.utils.PermissionHelper
import com.example.messenger.viewModels.MainActivityViewModel

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NewChatFragment : BottomSheetDialogFragment() {

    private var binding: FragmentNewChatBinding? = null
    private lateinit var adapter: ContactItemAdapter
    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null
    private val onSearchTextChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            val text = editable.toString().lowercase()

            val model: MainActivityViewModel by activityViewModels()
            val search = model.chats.value.filter {
                it.contact.name?.lowercase()?.contains(text) == true
            }
            adapter.refreshAdapter(search)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewChatBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val model: MainActivityViewModel by activityViewModels()

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    refreshContacts(model)
                }
            }

        initRV(model)
        initToolbar(model)
        initSearch()
    }

    /**
     * Initializes search bar.
     */
    private fun initSearch() {
        binding?.searchET?.addTextChangedListener(onSearchTextChangeListener)
    }

    /**
     * If contacts empty, show vector image hint, else display RV.
     */
    private fun contactsEmptyCheck(model: MainActivityViewModel) {
        if (model.chats.value.isEmpty()) {
            binding?.apply {
                newChatRV.isGone = true
                emptyContactsIV.root.isVisible = true
            }
        } else {
            binding?.apply {
                newChatRV.isVisible = true
                emptyContactsIV.root.isGone = true
            }
        }
    }

    /**
     * Initializes toolbar.
     */
    private fun initToolbar(model: MainActivityViewModel) {
        binding?.backButton?.setOnClickListener {
            this.dismiss()
        }
        binding?.refreshButton?.setOnClickListener {
            PermissionHelper.checkContactsPermission(requireActivity(), requestPermissionLauncher) {
                refreshContacts(model)
            }
        }
    }

    /**
     * Refreshes the contacts and updates the adapter.
     */
    private fun refreshContacts(model: MainActivityViewModel) {
        binding?.refreshPB?.isVisible = true
        binding?.newChatRV?.isGone = true
        binding?.emptyContactsIV?.root?.isGone = true
        lifecycleScope.launch(Dispatchers.IO) {
            model.updateContactsFromPhoneContacts()
            withContext(Dispatchers.Main) {
                binding?.refreshPB?.isGone = true
                binding?.newChatRV?.isVisible = true
                val contacts = model.chats.value.filter {
                    it.contact.name != null
                }
                adapter.refreshAdapter(contacts)
                contactsEmptyCheck(model)
            }
        }
    }

    /**
     * Initializes RV and filters unsaved contacts.
     */
    private fun initRV(model: MainActivityViewModel) {
        binding?.refreshPB?.isGone = true
        binding?.newChatRV?.isVisible = true
        val contacts = model.chats.value.filter {
            it.contact.name != null
        }
        adapter = ContactItemAdapter(contacts, this::openChatWithContact)
        binding?.apply {
            newChatRV.adapter = adapter
            newChatRV.layoutManager = LinearLayoutManager(requireContext())
        }
        contactsEmptyCheck(model)
    }

    private fun openChatWithContact(phoneNumber: String) {
        Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, phoneNumber)
            startActivity(this)
        }
        this.dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setOnShowListener { bottomSheetDialog ->
            bottomSheetDialog as BottomSheetDialog

            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.searchET?.removeTextChangedListener(onSearchTextChangeListener)
        binding = null
        requestPermissionLauncher = null
    }

}