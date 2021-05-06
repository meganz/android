package mega.privacy.android.app.meeting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Constants

class VideoListViewAdapter(
    private val inMeetingViewModel: InMeetingViewModel,
    private val listView: RecyclerView
) : ListAdapter<Participant, VideoListViewHolder>(ParticipantDiffCallback()) {

    override fun onViewRecycled(holder: VideoListViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycle()
    }

    private fun getParticipantPosition(peerId: Long, clientId: Long) =
        currentList.indexOfFirst { it.peerId == peerId && it.clientId == clientId }

    override fun onBindViewHolder(listHolder: VideoListViewHolder, position: Int) {
        listHolder.bind(inMeetingViewModel, getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return VideoListViewHolder(ItemParticipantVideoBinding.inflate(inflater, parent, false))
    }

    fun getHolder(position: Int): VideoListViewHolder {
        return listView.findViewHolderForAdapterPosition(position) as VideoListViewHolder
    }

    /**
     * Update participant privileges
     *
     * @param participant
     */
    fun updateParticipantPrivileges(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updatePrivilegeIcon(participant)
    }

    fun updateParticipantRes(participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateRes(participant)
    }

    /**
     * Update participant audio or video flags
     *
     * @param participant
     */
    fun updateParticipantAudioVideo(typeChange: Int, participant: Participant) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).let {
            if(typeChange == Constants.TYPE_VIDEO){
                it.updateVideo(participant)
            }else if(typeChange == Constants.TYPE_AUDIO){
                it.updateAudioIcon(participant)
            }

        }
    }

    /**
     * Update participant on hold session
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateSessionOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateSessionOnHold(participant, isOnHold)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     */
    fun updateCallOnHold(participant: Participant, isOnHold: Boolean) {
        val position = getParticipantPosition(participant.peerId, participant.clientId)
        getHolder(position).updateCallOnHold(participant, isOnHold)
    }

}
