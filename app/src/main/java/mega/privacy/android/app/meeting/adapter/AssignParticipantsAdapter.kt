package mega.privacy.android.app.meeting.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemAssignModeratorBinding

class AssignParticipantsAdapter(private val select: ((Int) -> Unit)) :
    ListAdapter<Participant, AssignParticipantsAdapter.AssignParticipantViewHolder>(
        AssignParticipantDiffCallback()
    ) {
    override fun onBindViewHolder(holder: AssignParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignParticipantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return AssignParticipantViewHolder(
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

    inner class AssignParticipantViewHolder(private val binding: ItemAssignModeratorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: Participant) {
            binding.name.text = participant.name

            if (participant.isSelected) {
                binding.avatar.setActualImageResource(R.drawable.ic_select_folder)
            } else {
                // Set actual avatar
                binding.avatar.setActualImageResource(R.drawable.ic_account_expired)
            }

            binding.assignLayout.setOnClickListener {
                select.invoke(adapterPosition)
            }
        }
    }
}

class AssignParticipantDiffCallback : DiffUtil.ItemCallback<Participant>() {
    override fun areItemsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        val result = (oldItem.name == newItem.name) && (oldItem.isSelected == newItem.isSelected)
        Log.d("ParticipantDiffCallback", "result 1 = $result")
        return result
    }

    override fun areContentsTheSame(oldItem: Participant, newItem: Participant): Boolean {
        val result = oldItem == newItem
        Log.d("ParticipantDiffCallback", "result 2 = $result")
        return result
    }
}
