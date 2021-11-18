package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemParticipantChatListBinding
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel

/**
 * RecyclerView's ListAdapter to show participants list.
 *
 * @property sharedModel  MeetingActivityViewModel, the activity view model related to meetings
 * @property select       Callback to be called when participant item is clicked
 */
class AssignParticipantsAdapter(
    private val sharedModel: MeetingActivityViewModel,
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
            sharedModel, select,
            ItemParticipantChatListBinding.inflate(
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

