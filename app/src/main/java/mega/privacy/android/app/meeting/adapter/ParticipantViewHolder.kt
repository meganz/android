package mega.privacy.android.app.meeting.adapter

import android.graphics.PorterDuff
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantViewHolder(
    private val binding: ItemParticipantChatListBinding,
    private val onParticipantOption: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.participantListThreeDots.setOnClickListener {
            onParticipantOption(bindingAdapterPosition)
        }

        binding.verifiedIcon.isVisible = false
        binding.participantListPermissions.isVisible = false
        binding.participantListContent.isVisible = false
        binding.participantListAudio.isVisible = true
        binding.participantListVideo.isVisible = true
    }

    fun bind(participant: Participant) {
        binding.participantListThumbnail.setImageBitmap(participant.avatar)
        binding.participantListName.text = participant.name
        binding.participantListName.text =
            participant.getDisplayName(binding.participantListName.context)

        val params = binding.participantListNameRl.layoutParams as ConstraintLayout.LayoutParams
        params.endToStart = binding.participantListAudio.id
        params.bottomToBottom = binding.participantListItemLayout.id
        params.topToTop = binding.participantListItemLayout.id
        params.startToEnd = binding.participantListThumbnail.id
        binding.participantListNameRl.requestLayout()

        if (participant.isModerator) {
            val drawable = ContextCompat.getDrawable(
                binding.participantListName.context,
                R.drawable.ic_moderator
            )
            drawable?.setTint(
                ContextCompat.getColor(
                    binding.participantListName.context,
                    R.color.teal_300_teal_200
                )
            )
            binding.participantListName.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                drawable,
                null
            )
            binding.participantListName.compoundDrawablePadding = 5
        } else {
            binding.participantListName.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                null,
                null
            )
        }

        if (participant.isAudioOn) {
            binding.participantListAudio.setImageResource(R.drawable.ic_mic_on)
            binding.participantListAudio.setColorFilter(
                ContextCompat.getColor(
                    binding.participantListAudio.context,
                    R.color.grey_054_white_054
                ),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.participantListAudio.setImageResource(R.drawable.ic_mic_off_grey_red)
            binding.participantListAudio.colorFilter = null
        }

        if (participant.isVideoOn) {
            binding.participantListVideo.setImageResource(R.drawable.ic_video)
            binding.participantListVideo.setColorFilter(
                ContextCompat.getColor(
                    binding.participantListVideo.context,
                    R.color.grey_054_white_054
                ),
                PorterDuff.Mode.SRC_IN
            )

        } else {
            binding.participantListVideo.setImageResource(R.drawable.ic_video_off_grey_red)
            binding.participantListVideo.colorFilter = null
        }
    }
}
