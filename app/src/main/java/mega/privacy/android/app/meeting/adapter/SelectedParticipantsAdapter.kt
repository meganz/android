package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.CallUtil

class SelectedParticipantsAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val delete: ((Participant) -> Unit)
) : ListAdapter<Participant, SelectedParticipantsAdapter.SelectedParticipantViewHolder>(
        AssignParticipantDiffCallback()
    ) {
    override fun onBindViewHolder(holder: SelectedParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SelectedParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SelectedParticipantViewHolder(
            ItemSelectedParticipantBinding.inflate(
                inflater,
                parent,
                false
            )
        )
    }

    inner class SelectedParticipantViewHolder(private val binding: ItemSelectedParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: Participant) {
            binding.nameChip.text = participant.name
            initAvatar(participant,binding.avatar)
            binding.itemLayoutChip.setOnClickListener {
                delete.invoke(getItem(adapterPosition))
            }
        }
    }

    private fun initAvatar(participant: Participant, avatarView: SimpleDraweeView) {
        inMeetingViewModel.getChat().let {
            var avatar = CallUtil.getImageAvatarCall(it, participant.peerId)
            if (avatar == null) {
                avatar = CallUtil.getDefaultAvatarCall(
                    MegaApplication.getInstance().applicationContext,
                    participant.peerId
                )
            }

            avatarView.setImageBitmap(avatar)
        }
    }
}
