package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class VideoGridViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val pagePosition: Int
) : ListAdapter<Participant, VideoGridViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holderGrid: VideoGridViewHolder, position: Int) {
        holderGrid.bind(inMeetingViewModel, getItem(position), itemCount, pagePosition == 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoGridViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoGridViewHolder(
            ItemParticipantVideoBinding.inflate(inflater, parent, false),
            gridView,
            screenWidth,
            screenHeight
        )
    }
}
