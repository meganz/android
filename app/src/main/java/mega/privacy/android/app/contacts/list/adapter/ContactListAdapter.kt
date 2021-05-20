package mega.privacy.android.app.contacts.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactDataBinding
import mega.privacy.android.app.databinding.ItemContactHeaderBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class ContactListAdapter(
    private val itemCallback: (String) -> Unit,
    private val itemInfoCallback: (String) -> Unit
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(ContactItem.DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATA = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                // Header row
                val binding = ItemContactHeaderBinding.inflate(layoutInflater, parent, false)
                ContactListHeaderViewHolder(binding)
            }
            else -> {
                // Data row
                val binding = ItemContactDataBinding.inflate(layoutInflater, parent, false)
                ContactListDataViewHolder(binding).apply {
                    binding.root.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val email = (getItem(bindingAdapterPosition) as ContactItem.Data).email
                            itemCallback.invoke(email)
                        }
                    }
                    binding.btnMore.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val email = (getItem(bindingAdapterPosition) as ContactItem.Data).email
                            itemInfoCallback.invoke(email)
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ContactListHeaderViewHolder -> holder.bind(getItem(position) as ContactItem.Header)
            is ContactListDataViewHolder -> holder.bind(getItem(position) as ContactItem.Data)
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ContactItem.Header -> VIEW_TYPE_HEADER
            is ContactItem.Data -> VIEW_TYPE_DATA
        }

    fun submitList(
        items: List<ContactItem>?,
        mainHeaderTitle: String,
        commitCallback: Runnable? = null
    ) {
        val itemsWithHeader = mutableListOf<ContactItem>()

        if (!items.isNullOrEmpty()) {
            itemsWithHeader.add(ContactItem.Header(mainHeaderTitle))
            itemsWithHeader.addAll(items)
        }

        submitList(itemsWithHeader, commitCallback)
    }
}
