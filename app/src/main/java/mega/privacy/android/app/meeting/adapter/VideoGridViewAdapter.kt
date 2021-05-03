package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.meeting.listeners.GridViewListener

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val pagePosition: Int,
    private val listener: GridViewListener
) : ListAdapter<Participant, VideoGridViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(gridHolder: VideoGridViewHolder, position: Int) {
        gridHolder.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoGridViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoGridViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            gridView,
            screenWidth,
            screenHeight,
            listener
        )
    }
}
