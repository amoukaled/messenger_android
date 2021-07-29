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

package com.example.messenger.adapters

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import com.example.messenger.data.local.relations.ContactWithMessages
import com.example.messenger.databinding.ContactItemBinding
import com.example.messenger.utils.Constants

class ContactItemAdapter(
    private var contacts: List<ContactWithMessages>,
    private val openChat: ((String) -> Unit)?
) :
    RecyclerView.Adapter<ContactItemAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
            ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact, openChat)
    }

    override fun getItemCount(): Int = contacts.size

    class ContactDiffUtilCallback(
        private val oldList: List<ContactWithMessages>,
        private val newList: List<ContactWithMessages>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].contact.phoneNumber == newList[newItemPosition].contact.phoneNumber
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    /**
     * Refreshes the adapter using [ContactDiffUtilCallback].
     */
    fun refreshAdapter(data: List<ContactWithMessages>) {
        val oldList = contacts
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            ContactDiffUtilCallback(
                oldList,
                data
            )
        )

        contacts = data
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * ViewHolder class for the contact.
     * @property binding The view binding class for the layout.
     */
    class ContactViewHolder(private val binding: ContactItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the data to the layout.
         * @param openChat Opens the chat in another activity.
         */
        fun bind(contact: ContactWithMessages, openChat: ((String) -> Unit)?) {
            with(binding) {
                // name
                contactNameTV.text = contact.contact.name

                // status
                contact.contact.status?.let {
                    contactStatusTV.text = it
                }

                // avatar
                contact.contact.getProfilePicRoundedDrawable(
                    contactAvatarIV.context,
                    Constants.MEDIUM_AVATAR_WIDTH,
                    Constants.MEDIUM_AVATAR_HEIGHT,
                ).let {
                    contactAvatarIV.setImageDrawable(it)
                }

                // Opens the chat with a callback passed to the adapter that
                // closes the bottom sheet after launching the chat activity.
                root.setOnClickListener {
                    openChat?.invoke(contact.contact.phoneNumber)
                }
            }
        }
    }

}