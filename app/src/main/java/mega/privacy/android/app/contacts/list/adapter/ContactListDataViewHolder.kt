package mega.privacy.android.app.contacts.list.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactDataBinding
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * RecyclerView's ViewHolder to show ContactItem Data info.
 *
 * @property binding    Item's view binding
 */
class ContactListDataViewHolder(
    private val binding: ItemContactDataBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactItem.Data) {
        binding.txtName.text = item.getTitle()
        binding.txtLastSeen.text = item.lastSeen
        binding.txtLastSeen.isVisible = !item.lastSeen.isNullOrBlank()
        binding.chipNew.isVisible = item.isNew
        binding.verifiedIcon.isVisible = item.isVerified
        binding.imgThumbnail.hierarchy.setPlaceholderImage(item.placeholder)
        if (item.avatarUri != null) {
            binding.imgThumbnail.setImageRequestFromUri(item.avatarUri)
        } else {
            binding.imgThumbnail.setImageRequest(null)
        }
        if (item.statusColor != null) {
            val color = ContextCompat.getColor(itemView.context, item.statusColor)
            binding.imgState.setColorFilter(color)
        } else {
            binding.imgState.clearColorFilter()
        }
    }
}
