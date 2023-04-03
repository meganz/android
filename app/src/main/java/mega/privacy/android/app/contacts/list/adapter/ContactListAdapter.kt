package mega.privacy.android.app.contacts.list.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactDataBinding
import mega.privacy.android.app.databinding.ItemContactHeaderBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

/**
 * RecyclerView's ListAdapter to show ContactItem.
 *
 * @property itemCallback       Callback to be called when the view item is clicked.
 * @property itemInfoCallback   Callback to be called when the avatar is clicked.
 * @property itemMoreCallback   Callback to be called when the "more button" is clicked.
 */
class ContactListAdapter(
    private val itemCallback: (Long) -> Unit,
    private val itemInfoCallback: (String) -> Unit,
    private val itemMoreCallback: (Long) -> Unit,
) : ListAdapter<ContactItem, RecyclerView.ViewHolder>(ContactItem.DiffCallback()),
    SectionTitleProvider {

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
                            val handle =
                                (getItem(bindingAdapterPosition) as ContactItem.Data).handle
                            itemCallback.invoke(handle)
                        }
                    }
                    binding.imgThumbnail.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val email = (getItem(bindingAdapterPosition) as ContactItem.Data).email
                            itemInfoCallback.invoke(email)
                        }
                    }
                    binding.btnMore.setOnClickListener {
                        if (isValidPosition(bindingAdapterPosition)) {
                            val handle =
                                (getItem(bindingAdapterPosition) as ContactItem.Data).handle
                            itemMoreCallback.invoke(handle)
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

    override fun getSectionTitle(position: Int, context: Context): String =
        getItem(position).getSection()
}
