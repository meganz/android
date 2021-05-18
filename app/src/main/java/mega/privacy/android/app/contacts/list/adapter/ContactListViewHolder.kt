package mega.privacy.android.app.contacts.list.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding

class ContactListViewHolder(
    private val binding: ItemContactBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactItem) {
        binding.txtName.text = item.firstName
        binding.txtHeader.text = item.getFirstCharacter()
        binding.txtLastSeen.text = item.lastSeen
        binding.chipNew.isVisible = item.isNew
        binding.imgThumbnail.hierarchy.setPlaceholderImage(item.getPlaceholderDrawable(itemView.resources))
        binding.imgThumbnail.setImageRequest(ImageRequest.fromUri(item.imageUri))
        item.statusColor?.let { binding.imgState.setColorFilter(ContextCompat.getColor(itemView.context, it)) }
    }
}
