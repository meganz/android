package mega.privacy.android.app.meeting.list.adapter

import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
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

    private val selectAnimation by lazy {
        AnimationUtils.loadAnimation(itemView.context, R.anim.multiselect_flip)
    }

    fun bind(item: MeetingItem, isSelected: Boolean) {
        binding.txtTitle.text = item.title
        binding.txtTimestamp.text = item.formattedTimestamp
        binding.txtLastMessage.text = item.lastMessage
        binding.txtLastMessage.isVisible = !item.lastMessage.isNullOrBlank()
        binding.imgMute.isVisible = item.isMuted
        binding.imgPrivate.isVisible = !item.isPublic
        binding.txtUnreadCount.text = item.unreadCount.toString()
        binding.txtUnreadCount.isVisible = item.unreadCount > 0
        val lastMessageColor = if (item.highlight) {
            ContextCompat.getColor(itemView.context, R.color.teal_300_teal_200)
        } else {
            getThemeColor(itemView.context, android.R.attr.textColorSecondary)
        }
        binding.txtLastMessage.setTextColor(lastMessageColor)

        val firstUserPlaceholder = item.firstUser.getImagePlaceholder(itemView.context)
        if (item.isSingleMeeting() || item.lastUser == null) {
            binding.imgThumbnail.hierarchy.setPlaceholderImage(
                firstUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.imgThumbnail.setImageRequestFromUri(item.firstUser.avatar)
            binding.groupThumbnails.isVisible = false
            binding.imgThumbnail.isVisible = !isSelected
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
            binding.groupThumbnails.isVisible = !isSelected
            binding.imgThumbnail.isVisible = false
        }

        binding.imgSelectState.apply {
            if ((isSelected && !isVisible) || (!isSelected && isVisible)) {
                isVisible = true
                startAnimation(selectAnimation.apply {
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            isVisible = isSelected
                        }

                        override fun onAnimationStart(animation: Animation?) {
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                        }
                    })
                })
            } else {
                isVisible = isSelected
            }
        }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int = bindingAdapterPosition
            override fun getSelectionKey(): Long = itemId
        }
}
