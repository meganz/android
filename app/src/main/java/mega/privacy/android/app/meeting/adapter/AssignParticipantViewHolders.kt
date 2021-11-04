package mega.privacy.android.app.meeting.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemAssignModeratorBinding
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class AssignParticipantViewHolder(
    private val viewModel: InMeetingViewModel,
    private val select: ((Int) -> Unit),
    private val binding: ItemAssignModeratorBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(participant: Participant) {
        binding.name.text = participant.name

        if (participant.isChosenForAssign) {
            binding.avatar.setImageResource(R.drawable.ic_select_folder)
        } else {
            // Set actual avatar
            binding.avatar.setImageBitmap(viewModel.getAvatarBitmapByPeerId(participant.peerId))
        }

        binding.assignLayout.setOnClickListener {
            select.invoke(bindingAdapterPosition)
        }
    }
}

class SelectedParticipantViewHolder(
    private val viewModel: InMeetingViewModel,
    private val delete: (Participant) -> Unit,
    private val binding: ItemSelectedParticipantBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(participant: Participant) {
        binding.nameChip.text = participant.name
        binding.avatar.setImageBitmap(viewModel.getAvatarBitmapByPeerId(participant.peerId))
        binding.itemLayoutChip.setOnClickListener {
            delete.invoke(participant)
        }
    }
}