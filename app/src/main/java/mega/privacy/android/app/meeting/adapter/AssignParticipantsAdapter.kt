package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemAssignModeratorBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class AssignParticipantsAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val select: ((Int) -> Unit)
) : ListAdapter<Participant, AssignParticipantViewHolder>(
    AssignParticipantDiffCallback()
) {
    override fun onBindViewHolder(holder: AssignParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return AssignParticipantViewHolder(
            inMeetingViewModel, select,
            ItemAssignModeratorBinding.inflate(
                inflater,
                parent,
                false
            )
        )
    }

    fun updateList(list: MutableList<Participant>?) {
        list?.let {
            val newList = mutableListOf<Participant>()
            newList.addAll(it)
            submitList(newList)
        }
    }


}

