package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel

class VideoListViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val itemClickViewModel: ItemClickViewModel
) : ListAdapter<Participant, VideoListViewHolder>(ParticipantDiffCallback()) {

    override fun onViewRecycled(holder: VideoListViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycle()
    }

    override fun onBindViewHolder(listHolder: VideoListViewHolder, position: Int) {
        listHolder.bind(inMeetingViewModel, itemClickViewModel, getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListViewHolder(ItemParticipantVideoBinding.inflate(inflater, parent, false))
    }
}
