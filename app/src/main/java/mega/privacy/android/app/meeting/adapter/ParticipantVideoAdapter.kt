package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding

class ParticipantVideoAdapter(
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int
) : ListAdapter<Participant, ParticipantVideoViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holder: ParticipantVideoViewHolder, position: Int) {
        holder.bind(getItem(position), itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantVideoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ParticipantVideoViewHolder(
            ItemCameraGroupCallBinding.inflate(inflater, parent, false),
            gridView,
            screenWidth,
            screenHeight
        )
    }
}
