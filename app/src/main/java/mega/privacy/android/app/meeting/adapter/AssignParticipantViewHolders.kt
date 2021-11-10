package mega.privacy.android.app.meeting.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Util
import org.jetbrains.anko.displayMetrics

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
            binding.participantListThumbnail.setImageResource(R.drawable.ic_select_folder)
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