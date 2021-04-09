package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding

class VideoListViewAdapter(
    private val itemClickViewModel: ItemClickViewModel
) : ListAdapter<Participant, VideoListViewHolder>(ParticipantDiffCallback()) {

    override fun onBindViewHolder(holderList: VideoListViewHolder, position: Int) {
        holderList.bind(getItem(position), itemClickViewModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListViewHolder(ItemCameraGroupCallBinding.inflate(inflater, parent, false))
    }
}
