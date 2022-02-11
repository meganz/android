package mega.privacy.android.app.meeting.adapter

import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import nz.mega.sdk.MegaChatApiJava

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantViewHolder(
    private val binding: ItemParticipantChatListBinding,
    private val onParticipantOption: (Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var peerId = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var clientId = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    lateinit var viewModel: InMeetingViewModel

    init {
        binding.participantListThreeDots.setOnClickListener(null)
        binding.participantListThreeDots.alpha = BUTTON_DISABLED
        binding.verifiedIcon.isVisible = false
        binding.participantListPermissions.isVisible = false
        binding.participantListContent.isVisible = false
        binding.callOptions.isVisible = true
    }

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant
    ) {

        this.viewModel = inMeetingViewModel

        peerId = participant.peerId
        clientId = participant.clientId

        checkParticipantsOptions(participant)

        binding.participantListThumbnail.setImageBitmap(participant.avatar)
        binding.participantListName.text =
            participant.getDisplayName(binding.participantListName.context)

        if (!participant.isModerator) {
            binding.participantListIconEnd.isVisible = false
        } else {
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
            binding.participantListIconEnd.isVisible = true
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

    /**
     * Updating the visibility of participants' options
     *
     * @param participant Participant from who will control whether his 3 dots should be shown or not
     */
    fun checkParticipantsOptions(participant: Participant) {
        if (participant.peerId != peerId || participant.clientId != clientId) return

        if (this.viewModel.shouldParticipantsOptionBeVisible(participant)) {
            binding.participantListThreeDots.setOnClickListener {
                onParticipantOption(bindingAdapterPosition)
            }

            binding.participantListThreeDots.alpha = BUTTON_ENABLED
        } else {
            binding.participantListThreeDots.setOnClickListener(null)
            binding.participantListThreeDots.alpha = BUTTON_DISABLED
        }
    }

    companion object {
        const val BUTTON_ENABLED = 1f
        const val BUTTON_DISABLED = 0.5f

    }
}
