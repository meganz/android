package mega.privacy.android.app.contacts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
    private val itemInfoCallback: (Long) -> Unit,
    private val enableHeaders: Boolean = true
) : ListAdapter<ContactItem, ContactsViewHolder>(ContactItem.DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ITEM_WITH_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactBinding.inflate(layoutInflater, parent, false)
        return ContactsViewHolder(binding).apply {
            binding.txtHeader.isVisible = viewType == VIEW_TYPE_ITEM_WITH_HEADER
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

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle

    override fun getItemViewType(position: Int): Int =
        when {
            !enableHeaders ->
                VIEW_TYPE_ITEM
            position == 0 ->
                VIEW_TYPE_ITEM_WITH_HEADER
            getItem(position).name?.firstOrNull()?.isLetter() == true &&
                getItem(position - 1).name?.firstOrNull() != getItem(position).name?.firstOrNull() ->
                VIEW_TYPE_ITEM_WITH_HEADER
            else ->
                VIEW_TYPE_ITEM
        }
}
