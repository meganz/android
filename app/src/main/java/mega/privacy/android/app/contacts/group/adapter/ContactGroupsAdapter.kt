package mega.privacy.android.app.contacts.group.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.ItemContactGroupBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactGroupsAdapter constructor(
    private val itemCallback: (Long) -> Unit
) : ListAdapter<ContactGroupItem, ContactGroupsViewHolder>(ContactGroupItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactGroupsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactGroupBinding.inflate(layoutInflater, parent, false)
        return ContactGroupsViewHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemCallback.invoke(getItem(bindingAdapterPosition).chatId)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactGroupsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).chatId
}
