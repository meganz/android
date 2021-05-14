package mega.privacy.android.app.contacts.requests.adapter

import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.databinding.ItemContactRequestBinding

class ContactRequestListViewHolder(
    private val binding: ItemContactRequestBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactRequestItem) {
        val isOutgoing = item.isOutgoing || item.name.isNullOrBlank()
        binding.txtTitle.text = if (isOutgoing) item.email else item.name
        binding.txtSubtitle.text = if (isOutgoing) item.createdTime else item.email
        binding.imgThumbnail.hierarchy.setPlaceholderImage(item.getPlaceholderDrawable(itemView.resources))
        binding.imgThumbnail.setImageRequest(ImageRequest.fromUri(item.imageUri))
    }
}
