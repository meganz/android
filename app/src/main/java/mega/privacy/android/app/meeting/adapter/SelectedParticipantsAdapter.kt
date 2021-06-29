package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class SelectedParticipantsAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val delete: ((Participant) -> Unit)
) : ListAdapter<Participant, SelectedParticipantViewHolder>(
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
            inMeetingViewModel, delete,
            ItemSelectedParticipantBinding.inflate(
                inflater,
                parent,
                false
            )
        )
    }
}
