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
import android.view.Menu
import android.view.MenuItem

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.messenger.R
import com.example.messenger.adapters.ChatItemAdapter
import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.databinding.ActivityMainBinding
import com.example.messenger.fragments.NewChatFragment
import com.example.messenger.utils.PermissionHelper
import com.example.messenger.viewModels.MainActivityViewModel

import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transform

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ChatItemAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val model: MainActivityViewModel by viewModels {
            viewModelFactory
        }
        model.updateToken()
        permissionsCheck(model)
        initRecyclerView(model)
        lifecycleScope.launchWhenCreated {
            collectFlowEvents(model)
        }

        // setting the action bar
        setSupportActionBar(binding.mainToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        initFAB()
    }

    /**
     * Initializes the FAB button to launch [NewChatFragment].
     */
    private fun initFAB() {
        binding.newChatFAB.setOnClickListener {
            NewChatFragment().apply {
                show(supportFragmentManager, "newChat")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return true
    }

    /**
     * Checks the required permissions and updates
     * the app contacts to get token, status and profile pic updates.
     */
    private fun permissionsCheck(model: MainActivityViewModel) {
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    model.updateDBContacts()
                }
            }

        PermissionHelper.checkContactsPermission(this, requestPermissionLauncher) {
            model.updateDBContacts()
        }
    }

    /**
     * Initializes the RecyclerView.
     */
    private fun initRecyclerView(model: MainActivityViewModel) {
        adapter = ChatItemAdapter(model.chats.value.toMutableList(), model)

        binding.apply {
            chatsRV.adapter = adapter
            chatsRV.layoutManager = LinearLayoutManager(this@MainActivity)
        }
        chatsCheckIfEmpty(model.chats.value)
    }

    /**
     * Collects events from the state flow and
     * refreshes the adapter.
     */
    private suspend fun collectFlowEvents(model: MainActivityViewModel) {
        model.chats.transform { chats ->
            val nonEmptyChats = chats.filter { !it.isChatEmpty() }
            emit(nonEmptyChats)
        }.collect {
            adapter.updateAdapter(it)
            chatsCheckIfEmpty(it)
        }
    }

    /**
     * Checks if the chats list is empty to display the
     * hint vector image.
     */
    private fun chatsCheckIfEmpty(list: List<ContactWithMessages>) {
        binding.apply {
            if (list.isEmpty()) {
                if (emptyChatsIV.root.isGone) {
                    emptyChatsIV.root.isVisible = true
                }
            } else {
                if (emptyChatsIV.root.isVisible) {
                    emptyChatsIV.root.isGone = true
                }
            }
        }
    }

    /**
     * Recollecting events after resume.
     */
    override fun onResume() {
        super.onResume()
        val model: MainActivityViewModel by viewModels()
        val nonEmptyChats = model.chats.value.filter { !it.isChatEmpty() }
        // force refreshing the RV
        adapter.forceRefresh(nonEmptyChats)
    }

    // Menu callbacks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logoutMI -> {
                val model: MainActivityViewModel by viewModels()
                val intent = Intent(this@MainActivity, AuthActivity::class.java)
                model.logout(this, intent)
            }
            R.id.profileMI -> {
                Intent(this@MainActivity, ProfileActivity::class.java).also { intent ->
                    startActivity(intent)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

}