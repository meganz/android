package mega.privacy.android.app.contacts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemInfoCallback: (Long) -> Unit,
) : ListAdapter<ContactItem, ContactsViewHolder>(ContactItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactBinding.inflate(layoutInflater, parent, false)

        return ContactsViewHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(adapterPosition)) {
                    itemCallback.invoke(getItem(adapterPosition).handle)
                }
            }

            binding.btnMore.setOnClickListener {
                if (isValidPosition(adapterPosition)) {
                    itemInfoCallback.invoke(getItem(adapterPosition).handle)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle
}
