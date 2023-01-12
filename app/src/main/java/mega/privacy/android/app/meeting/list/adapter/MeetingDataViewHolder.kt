package mega.privacy.android.app.meeting.list.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.drawable.ScalingUtils
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemMeetingDataBinding
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.setImageRequestFromFilePath
import mega.privacy.android.app.utils.view.TextDrawable

class MeetingDataViewHolder(
    private val binding: ItemMeetingDataBinding,
) : RecyclerView.ViewHolder(binding.root) {

    private val selectAnimation by lazy {
        AnimationUtils.loadAnimation(itemView.context, R.anim.multiselect_flip)
    }

    fun bind(item: MeetingAdapterItem.Data, isSelected: Boolean) {
        val room = item.room
        val lastMessageColor = when {
            room.isScheduledMeeting() -> ContextCompat.getColor(itemView.context, R.color.red_600_red_300)
            room.highlight -> ContextCompat.getColor(itemView.context, R.color.teal_300_teal_200)
            else -> getThemeColor(itemView.context, android.R.attr.textColorSecondary)
        }

        binding.txtTitle.text = room.title
        if (room.isScheduledMeeting()) {
            val scheduledTimeFormatted = room.scheduledTimestampFormatted
            val descriptionText = if (room.isRecurring) {
                itemView.context.getString(R.string.meetings_list_scheduled_meeting_weekly_label, scheduledTimeFormatted)
            } else {
                scheduledTimeFormatted
            }
            binding.txtLastMessage.text = descriptionText
            binding.txtLastMessage.isVisible = !scheduledTimeFormatted.isNullOrBlank()
            binding.imgRecurring.isVisible = room.isRecurring
            binding.txtTimestamp.setText(if (room.isRecurring) {
                R.string.meetings_list_recurring_meeting_label
            } else {
                R.string.meetings_list_upcoming_meeting_label
            })
        } else {
            binding.txtLastMessage.text = room.lastMessage
            binding.txtLastMessage.isVisible = !room.lastMessage.isNullOrBlank()
            binding.txtTimestamp.text = room.lastTimestampFormatted
        }
        binding.txtLastMessage.setTextColor(lastMessageColor)

        when {
            room.isLastMessageVoiceClip -> {
                binding.imgLastMessage.setImageResource(R.drawable.ic_mic_on_small)
                binding.imgLastMessage.isVisible = true
            }
            room.isLastMessageGeolocation -> {
                binding.imgLastMessage.setImageResource(R.drawable.ic_location_small)
                binding.imgLastMessage.isVisible = true
            }
            else -> {
                binding.imgLastMessage.setImageDrawable(null)
                binding.imgLastMessage.isVisible = false
            }
        }

        binding.imgLastMessage.imageTintList = ColorStateList.valueOf(lastMessageColor)
        binding.imgMute.isVisible = room.isMuted
        binding.imgPrivate.isVisible = !room.isPublic
        binding.txtUnreadCount.text = room.unreadCount.toString()
        binding.txtUnreadCount.isVisible = room.unreadCount > 0

        if (room.firstUserChar == null && room.lastUserChar == null) {
            binding.groupThumbnails.isVisible = false
            binding.imgThumbnail.isVisible = false
        } else {
            val firstUserPlaceholder = getImagePlaceholder(room.firstUserChar.toString(), room.firstUserColor)
            if (room.isSingleMeeting() || room.lastUserAvatar == null) {
                binding.imgThumbnail.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.imgThumbnail.setImageRequestFromFilePath(room.firstUserAvatar)
                binding.groupThumbnails.isVisible = false
                binding.imgThumbnail.isVisible = !isSelected
            } else {
                val lastUserPlaceholder = getImagePlaceholder(room.lastUserChar.toString(), room.lastUserColor)
                binding.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(
                    lastUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.imgThumbnailGroupFirst.setImageRequestFromFilePath(room.firstUserAvatar)
                binding.imgThumbnailGroupLast.setImageRequestFromFilePath(room.lastUserAvatar)
                binding.groupThumbnails.isVisible = !isSelected
                binding.imgThumbnail.isVisible = false
            }
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

    private fun getImagePlaceholder(letter: String?, avatarColor: Int?): Drawable? =
        if (!letter.isNullOrBlank() && avatarColor != null) {
            TextDrawable.builder()
                .beginConfig()
                .width(itemView.resources.getDimensionPixelSize(R.dimen.image_group_size))
                .height(itemView.resources.getDimensionPixelSize(R.dimen.image_group_size))
                .fontSize(itemView.resources.getDimensionPixelSize(R.dimen.image_group_text_size))
                .withBorder(itemView.resources.getDimensionPixelSize(R.dimen.image_group_border_size))
                .borderColor(ContextCompat.getColor(itemView.context, R.color.white_dark_grey))
                .bold()
                .toUpperCase()
                .endConfig()
                .buildRound(letter, avatarColor)
        } else {
            null
        }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition(): Int = bindingAdapterPosition
            override fun getSelectionKey(): Long = itemId
        }
}
