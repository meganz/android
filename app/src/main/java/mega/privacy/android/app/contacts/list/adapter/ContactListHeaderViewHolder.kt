package mega.privacy.android.app.contacts.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactHeaderBinding

class ContactListHeaderViewHolder(
    private val binding: ItemContactHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactItem.Header) {
        binding.txtHeader.text = item.title
    }
}
