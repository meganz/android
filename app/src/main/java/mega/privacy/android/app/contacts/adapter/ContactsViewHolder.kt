package mega.privacy.android.app.contacts.adapter

import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.RecyclerView
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.databinding.ItemContactBinding

class ContactsViewHolder(private val binding: ItemContactBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactItem) {
        binding.txtName.text = item.name
        binding.txtLastSeen.text = item.lastSeen
        binding.imgThumbnail.hierarchy.setPlaceholderImage(ColorDrawable(item.imageColor))
        binding.imgThumbnail.setImageRequest(ImageRequest.fromUri(item.imageUri))
    }
}
