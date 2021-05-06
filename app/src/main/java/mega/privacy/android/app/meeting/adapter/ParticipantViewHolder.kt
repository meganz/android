package mega.privacy.android.app.meeting.adapter

import android.graphics.PorterDuff
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemMeetingParticipantBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import java.util.*

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantViewHolder(
    private val inMeetingViewModel: InMeetingViewModel,
    private val binding: ItemMeetingParticipantBinding,
    private val onParticipantOption: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.threeDots.setOnClickListener {
            onParticipantOption(adapterPosition)
        }
    }

    fun bind(participant: Participant) {
        if (isFileAvailable(participant.avatar)) {
            binding.avatar.setImageURI(Uri.fromFile(participant.avatar))
        } else {
            initAvatar(participant)
        }

        binding.name.text = participant.name

        if (participant.isModerator) {
            binding.name.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(binding.name.context, R.drawable.ic_moderator),
                null
            )
        } else {
            binding.name.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        if (participant.isMe) {
            binding.name.text = HtmlCompat.fromHtml(
                "${participant.name} <font color='${
                    getColorHexString(binding.name.context, R.color.grey_600)
                }'>(${getString(R.string.bucket_word_me).toLowerCase(Locale.ROOT)})</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            binding.name.text = participant.name
        }

        if (participant.isAudioOn) {
            binding.audioStatus.setImageResource(R.drawable.ic_mic_on)
            binding.audioStatus.setColorFilter(
                ContextCompat.getColor(binding.audioStatus.context, R.color.grey_054_white_054),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.audioStatus.setImageResource(R.drawable.ic_mic_off_grey_red)
            binding.audioStatus.colorFilter = null
        }

        if (participant.isVideoOn) {
            binding.videoStatus.setImageResource(R.drawable.ic_video)
            binding.videoStatus.setColorFilter(
                ContextCompat.getColor(binding.videoStatus.context, R.color.grey_054_white_054),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.videoStatus.setImageResource(R.drawable.ic_video_off_grey_red)
            binding.videoStatus.colorFilter = null
        }
    }

    private fun initAvatar(participant: Participant) {
        inMeetingViewModel.getChat()?.let {
            var avatar = CallUtil.getImageAvatarCall(it, participant.peerId)
            if (avatar == null) {
                avatar = CallUtil.getDefaultAvatarCall(
                    MegaApplication.getInstance().applicationContext,
                    participant.peerId
                )
            }

            binding.avatar.setImageBitmap(avatar)
        }
    }
}
