package mega.privacy.android.app.meeting.list.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemMeetingBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.utils.setImageRequestFromUri

class MeetingsViewHolder(
    private val binding: ItemMeetingBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MeetingItem) {
        binding.txtTitle.text = item.title
        binding.txtLastMessage.text = item.lastMessage
        binding.txtTimestamp.text = item.formattedDate

        val firstUserPlaceholder = item.firstUser.getImagePlaceholder(itemView.context)
        if (item.isGroup) {
            val lastUserPlaceholder = item.lastUser!!.getImagePlaceholder(itemView.context)
            binding.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(firstUserPlaceholder)
            binding.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(lastUserPlaceholder)
            binding.imgThumbnailGroupFirst.setImageRequestFromUri(item.firstUser.avatar)
            binding.imgThumbnailGroupLast.setImageRequestFromUri(item.lastUser.avatar)
            binding.imgThumbnailGroupFirst.isVisible = true
            binding.imgThumbnailGroupLast.isVisible = true
            binding.imgThumbnail.isVisible = false
        } else {
            binding.imgThumbnail.hierarchy.setPlaceholderImage(firstUserPlaceholder)
            binding.imgThumbnail.setImageRequestFromUri(item.firstUser.avatar)
            binding.imgThumbnailGroupFirst.isVisible = false
            binding.imgThumbnailGroupLast.isVisible = false
            binding.imgThumbnail.isVisible = true
        }
    }
}
