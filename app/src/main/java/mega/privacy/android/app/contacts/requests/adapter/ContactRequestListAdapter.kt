package mega.privacy.android.app.contacts.requests.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.databinding.ItemContactRequestBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactRequestListAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemInfoCallback: (Long) -> Unit
) : ListAdapter<ContactRequestItem, ContactRequestListViewHolder>(ContactRequestItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactRequestListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactRequestBinding.inflate(layoutInflater, parent, false)
        return ContactRequestListViewHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
            binding.btnMore.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemInfoCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactRequestListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle
}
