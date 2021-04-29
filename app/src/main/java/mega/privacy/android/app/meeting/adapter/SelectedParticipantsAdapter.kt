package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemSelectedParticipantBinding

class SelectedParticipantsAdapter(private val delete: ((Participant) -> Unit)) :
    ListAdapter<Participant, SelectedParticipantsAdapter.SelectedParticipantViewHolder>(
        AssignParticipantDiffCallback()
    ) {
    override fun onBindViewHolder(holder: SelectedParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedParticipantViewHolder {
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

            binding.avatar.setActualImageResource(R.drawable.ic_select_folder)
            binding.itemLayoutChip.setOnClickListener {
                delete.invoke(getItem(adapterPosition))
            }
        }
    }
}
