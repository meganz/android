package mega.privacy.android.app.meeting.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Util
import org.jetbrains.anko.displayMetrics

/**
 * The view holder for the participant view on the Make moderator screen.
 *
 * @param sharedModel   MeetingActivityViewModel, the activity view model related to meetings
 * @param select        Callback to be called when participant item is clicked
 * @param binding       ItemParticipantChatListBinding
 */
class AssignParticipantViewHolder(
    private val sharedModel: MeetingActivityViewModel,
    private val select: ((Int) -> Unit),
    private val binding: ItemParticipantChatListBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(participant: Participant) {
        binding.participantListName.text = participant.name

        binding.participantListNameRl.setPadding(
            0,
            0,
            Util.scaleWidthPx(16, MegaApplication.getInstance().displayMetrics),
            0
        )

        if (participant.isChosenForAssign) {
            binding.participantListThumbnail.setImageResource(CoreUiR.drawable.ic_select_folder)
        } else {
            // Set actual avatar
            binding.participantListThumbnail.setImageBitmap(
                sharedModel.getAvatarBitmapByPeerId(
                    participant.peerId
                )
            )
        }

        binding.participantListItemLayout.setOnClickListener {
            select.invoke(bindingAdapterPosition)
        }

        binding.verifiedIcon.isVisible = false
        binding.participantListThreeDots.isVisible = false
        binding.participantListPermissions.isVisible = false
        binding.participantListAudio.isVisible = false
        binding.participantListVideo.isVisible = false
        binding.participantListContent.isVisible = false
    }
}

/*
 * This class is in charge of managing the participants that are selected to change their permissions to MODERATOR.
 */
class SelectedParticipantViewHolder(
    private val sharedModel: MeetingActivityViewModel,
    private val delete: (Participant) -> Unit,
    private val binding: ItemSelectedParticipantBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(participant: Participant) {
        binding.nameChip.text = participant.name
        binding.avatar.setImageBitmap(sharedModel.getAvatarBitmapByPeerId(participant.peerId))
        binding.itemLayoutChip.setOnClickListener {
            delete.invoke(participant)
        }
    }
}