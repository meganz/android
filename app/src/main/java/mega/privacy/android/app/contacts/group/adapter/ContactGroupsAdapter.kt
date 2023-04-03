package mega.privacy.android.app.contacts.group.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.ItemContactGroupBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.AvatarUtil

/**
 * RecyclerView's ListAdapter to show ContactGroupItem.
 *
 * @property itemCallback   Callback to be called when an item is clicked.
 */
class ContactGroupsAdapter constructor(
    private val itemCallback: (Long) -> Unit,
) : ListAdapter<ContactGroupItem, ContactGroupsViewHolder>(ContactGroupItem.DiffCallback()),
    SectionTitleProvider {

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

    override fun getSectionTitle(position: Int, context: Context): String =
        AvatarUtil.getFirstLetter(getItem(position).title)
}
