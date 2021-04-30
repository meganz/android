package mega.privacy.android.app.contacts.adapter

import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding

class ContactsViewHolder(
    private val binding: ItemContactBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactItem) {
        binding.txtName.text = item.name
        binding.txtHeader.text = item.name?.firstOrNull()?.toString()
        binding.txtLastSeen.text = item.lastSeen
        binding.imgThumbnail.hierarchy.setPlaceholderImage(item.imageColor.toDrawable())
        binding.imgThumbnail.setImageRequest(ImageRequest.fromUri(item.imageUri))
        binding.imgState.setColorFilter(ContextCompat.getColor(itemView.context, item.statusColor))
    }
}
