package mega.privacy.android.app.contacts.requests.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.databinding.ItemContactRequestBinding
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * RecyclerView's ViewHolder to show ContactRequestItem.
 *
 * @property binding    Item's view binding
 */
class ContactRequestListViewHolder(
    private val binding: ItemContactRequestBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactRequestItem) {
        binding.txtTitle.text = item.email
        binding.txtSubtitle.text = item.createdTime
        binding.imgThumbnail.hierarchy.setPlaceholderImage(item.placeholder)
        binding.imgThumbnail.setImageRequestFromUri(item.avatarUri)
    }
}
