package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatSession

class SpeakerViewCallFragment : MeetingBaseFragment() {

    lateinit var adapter: VideoListViewAdapter

    private var participants: MutableList<Participant> = mutableListOf()

    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it
        adapter.submitList(it)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.speaker_view_call_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)
        lm.orientation = LinearLayoutManager.HORIZONTAL

        participants_horizontal_list.apply {
            layoutManager = lm
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            clipToPadding = true
            setHasFixedSize(true)
        }

        (parentFragment as InMeetingFragment).inMeetingViewModel.pinItemEvent.observe(viewLifecycleOwner, EventObserver {
//            speaker_video.setBackgroundColor(Color.parseColor(it.avatarBackground))
        })

        adapter = VideoListViewAdapter((parentFragment as InMeetingFragment).inMeetingViewModel, participants_horizontal_list)
        participants_horizontal_list.adapter = adapter

        // TODO test code start
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
            participantsObserver
        )
        // TODO test code end
    }

    private fun getParticipant(peerId: Long, clientId: Long): Participant? {
        val participant = participants.filter {
            it.peerId == peerId && it.clientId == clientId
        }
        if (participant.isNotEmpty()) {
            return participant[0]
        }
        return null
    }

    fun updateRemoteAudioVideo(type: Int, session: MegaChatSession) {
        getParticipant(session.peerid, session.clientid)?.let {
            adapter.updateParticipantAudioVideo(type, it)
        }
    }

    fun updateSessionOnHold(session: MegaChatSession) {
        getParticipant(session.peerid, session.clientid)?.let {
            adapter.updateSessionOnHold(it, session.isOnHold)
        }
    }

    fun updateRes(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            getParticipant(peer.peerId, peer.clientId)?.let {
                adapter.updateParticipantRes(it)
            }
        }
    }

    fun updateCallOnHold(isCallOnHold: Boolean) {
        val iterator = participants.iterator()
        iterator.forEach {
            adapter.updateCallOnHold(it, isCallOnHold)
        }
    }

    fun updatePrivileges(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            getParticipant(peer.peerId, peer.clientId)?.let {
                adapter.updateParticipantPrivileges(it)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
    }

    companion object {

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }
}