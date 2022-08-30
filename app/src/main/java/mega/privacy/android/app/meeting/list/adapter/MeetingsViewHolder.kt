package mega.privacy.android.app.meeting.list.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.ScalingUtils
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemMeetingBinding
import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.setImageRequestFromUri

class MeetingsViewHolder(
    private val binding: ItemMeetingBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: MeetingItem) {
        binding.txtTitle.text = item.title
        binding.txtTimestamp.text = item.formattedTimestamp
        binding.txtLastMessage.text = item.lastMessage
        binding.txtLastMessage.isVisible = !item.lastMessage.isNullOrBlank()
        binding.imgMute.isVisible = item.isMuted
        binding.imgPrivate.isVisible = !item.isPublic
        binding.txtUnreadCount.text = item.unreadCount.toString()

        val textColor: Int
        val showUnreadCount: Boolean
        if (item.unreadCount > 0) {
            showUnreadCount = true
            textColor = ContextCompat.getColor(itemView.context, R.color.teal_300_teal_200)
        } else {
            showUnreadCount = false
            textColor = getThemeColor(itemView.context, android.R.attr.textColorSecondary)
        }
        binding.txtUnreadCount.isVisible = showUnreadCount
        binding.txtLastMessage.setTextColor(textColor)

        val firstUserPlaceholder = item.firstUser.getImagePlaceholder(itemView.context)
        if (item.isSingleMeeting() || item.lastUser == null) {
            binding.imgThumbnail.hierarchy.setPlaceholderImage(
                firstUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.imgThumbnail.setImageRequestFromUri(item.firstUser.avatar)
            binding.groupThumbnails.isVisible = false
            binding.imgThumbnail.isVisible = true
        } else {
            val lastUserPlaceholder = item.lastUser.getImagePlaceholder(itemView.context)
            binding.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(
                firstUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(
                lastUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.imgThumbnailGroupFirst.setImageRequestFromUri(item.firstUser.avatar)
            binding.imgThumbnailGroupLast.setImageRequestFromUri(item.lastUser.avatar)
            binding.groupThumbnails.isVisible = true
            binding.imgThumbnail.isVisible = false
        }
    }
}
