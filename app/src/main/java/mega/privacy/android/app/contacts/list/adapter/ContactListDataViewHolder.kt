package mega.privacy.android.app.contacts.list.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactDataBinding
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId

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
        binding.imgThumbnail.load(
            data = ContactAvatar(id = UserId(item.handle))
        ) {
            transformations(CircleCropTransformation())
            placeholder(item.placeholder)
        }
        if (item.statusColor != null) {
            val color = ContextCompat.getColor(itemView.context, item.statusColor)
            binding.imgState.setColorFilter(color)
        } else {
            binding.imgState.clearColorFilter()
        }
    }
}
