package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
import org.jetbrains.anko.displayMetrics

class SpeakerViewCallFragment : MeetingBaseFragment() {

    lateinit var adapter: VideoListViewAdapter


    private var participants: MutableList<Participant> = mutableListOf()

    private var speakerUser: Participant? = null

    private val localAudioLevelObserver = Observer<MegaChatCall> {
        when {
            (parentFragment as InMeetingFragment).inMeetingViewModel.isSameCall(it.callId) -> {
                speakerUser?.let { speaker ->
                    when {
                        (parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic -> {
                            when {
                                !speaker.isMe -> {
                                    (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(
                                        speaker
                                    )
                                    (parentFragment as InMeetingFragment).inMeetingViewModel.assignMeAsSpeaker()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val remoteAudioLevelObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                (parentFragment as InMeetingFragment).inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    speakerUser?.let { speaker ->
                        when {
                            (parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic &&
                                    (speaker.peerId != callAndSession.second.peerid ||
                                            speaker.clientId != callAndSession.second.clientid) -> {
                                when {
                                    speaker.isMe -> {
                                        closeLocalVideo(speaker)
                                    }
                                }

                                selectSpeaker(
                                    callAndSession.second.peerid,
                                    callAndSession.second.clientid
                                )
                            }
                        }
                    }
                }
            }
        }


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

        (parentFragment as InMeetingFragment).inMeetingViewModel.pinItemEvent.observe(
            viewLifecycleOwner,
            EventObserver {
                when {
                    (parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic -> {
                        (parentFragment as InMeetingFragment).inMeetingViewModel.setSpeakerSelection(
                            false
                        )
                    }
                    else -> {
                        speakerUser?.let { currentSpeaker ->
                            if (currentSpeaker.peerId == it.peerId && currentSpeaker.clientId == it.clientId) {
                                (parentFragment as InMeetingFragment).inMeetingViewModel.setSpeakerSelection(
                                    true
                                )
                            }
                        }
                    }
                }

                selectSpeaker(it.peerId, it.clientId)
            })

        //Init Speaker participant: Me
        (parentFragment as InMeetingFragment).inMeetingViewModel.assignMeAsSpeaker()

        adapter = VideoListViewAdapter(
            (parentFragment as InMeetingFragment).inMeetingViewModel,
            participants_horizontal_list
        )
        participants_horizontal_list.adapter = adapter

        // TODO test code start
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
            participantsObserver
        )
        // TODO test code end

        init()
    }

    private fun init() {
        LiveEventBus.get(Constants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE)
            .observeSticky(this, remoteAudioLevelObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_LOCAL_AUDIO_LEVEL_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAudioLevelObserver)

        (parentFragment as InMeetingFragment).inMeetingViewModel.speakerParticipant.observe(
            viewLifecycleOwner
        ) {
            it?.let {
                speakerUser = it
                updateSpeakerUser(it)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                when {
                    speakerParticipant.isMe -> {
                        when {
                            it -> {
                                activateLocalVideo(speakerParticipant)
                            }
                            else -> {
                                showAvatar(speakerParticipant)
                                checkCallOnHold(speakerParticipant)
                            }
                        }
                    }
                }
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                when {
                    speakerParticipant.isMe -> {
                        speaker_mute_icon.isVisible = !it
                    }
                }
            }
        }
    }

    /**
     * Method for selecting a new speaker
     *
     * @param peerId
     * @param clienId
     */
    private fun selectSpeaker(peerId: Long, clienId: Long) {
        val listParticipants =
            (parentFragment as InMeetingFragment).inMeetingViewModel.updatePeerSelected(
                peerId, clienId
            )

        when {
            listParticipants.isNotEmpty() -> {
                updateSpeakerPeers(listParticipants)
            }
        }
    }

    /**
     * Monitor changes when updating the speaker participant
     *
     * @param participant
     */
    private fun updateSpeakerUser(participant: Participant) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getAvatarBitmap(participant.peerId)
            ?.let {
                speaker_avatar_image.setImageBitmap(it)
            }

        speaker_mute_icon.isVisible = !participant.isAudioOn

        when {
            participant.isMe -> {
                when {
                    participant.isVideoOn -> {
                        activateLocalVideo(participant)
                    }
                    else -> {
                        showAvatar(participant)
                        checkCallOnHold(participant)
                    }
                }
            }
            else -> {
                if (participant.isVideoOn) {
                    activateRemoteVideo(participant)
                } else {
                    showAvatar(participant)
                    checkSessionOnHold(participant)
                }
            }
        }
    }

    /**
     * Method to activate local video
     *
     * @param participant
     */
    private fun activateLocalVideo(participant: Participant) {
        speaker_on_hold_icon.isVisible = false
        speaker_avatar_image.alpha = 1f
        speaker_avatar_image.isVisible = false
        speaker_video.isVisible = true

        when (participant.videoListener) {
            null -> {

                participant.videoListener = MeetingVideoListener(
                    speaker_video,
                    outMetrics,
                    MEGACHAT_INVALID_HANDLE,
                    false
                )

                sharedModel.addLocalVideo(
                    (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                    participant.videoListener
                )
            }
        }
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateRemoteVideo(participant: Participant) {
        speaker_avatar_image.isVisible = false
        speaker_video.isVisible = true

        when (participant.videoListener) {
            null -> {
                val vListener = MeetingVideoListener(
                    speaker_video,
                    MegaApplication.getInstance().applicationContext.displayMetrics,
                    participant.clientId,
                    false
                )

                participant.videoListener = vListener

                (parentFragment as InMeetingFragment).inMeetingViewModel.onActivateVideo(participant)
            }
        }
    }

    /**
     * Show Avatar and close the video
     *
     * @param participant The Speaker
     */
    private fun showAvatar(participant: Participant) {
        speaker_avatar_image.isVisible = true
        speaker_video.isVisible = false

        when {
            participant.isMe -> {
                closeLocalVideo(participant)
            }
            else -> {
                (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(participant)
            }
        }
    }

    /**
     * Method to close local video
     *
     * @param participant
     */
    private fun closeLocalVideo(participant: Participant) {
        when (participant.videoListener) {
            null -> return
            else -> {
                sharedModel.removeLocalVideo(
                    (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                    participant.videoListener
                )
                participant.videoListener = null
            }
        }
    }

    /**
     * Control the UI when the call is on hold
     *
     * @param participant The Speaker
     */
    private fun checkCallOnHold(participant: Participant) {
        when {
            (parentFragment as InMeetingFragment).inMeetingViewModel.isCallOnHold() -> {
                if (participant.isMe) {
                    speaker_on_hold_icon.isVisible = true
                }
                speaker_avatar_image.alpha = 0.5f
            }
            else -> {
                if (participant.isMe) {
                    speaker_on_hold_icon.isVisible = false
                }
                speaker_avatar_image.alpha = 1f
            }
        }
    }

    /**
     * Control the UI when the session is on hold
     *
     * @param participant The Speaker
     */
    private fun checkSessionOnHold(participant: Participant) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getSession(participant.clientId)
            ?.let { session ->
                when {
                    session.isOnHold -> {
                        speaker_on_hold_icon.isVisible = true
                        speaker_avatar_image.alpha = 0.5f
                    }
                    else -> {
                        speaker_on_hold_icon.isVisible = false
                        speaker_avatar_image.alpha = 1f
                    }
                }
            }
        checkCallOnHold(participant)
    }


    /**
     * Update the adapter if a participant joins or leaves the call
     *
     * @param isAdded if is aader or removed
     * @param position
     */
    fun peerAddedOrRemoved(isAdded: Boolean, position: Int) {
        when {
            isAdded -> {
                adapter.notifyItemInserted(position)
            }
            else -> {
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, participants.size)
            }
        }
    }

    /**
     * Update the participant who is speaking and the one who will no longer be the speaker.
     *
     * @param listPeers List of participants with changes
     */
    private fun updateSpeakerPeers(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                adapter.updatePeerSelected(it)
            }
        }
    }

    /**
     * Check changes in remote A/V flags
     *
     * @param type type of change, Audio or Video
     * @param session MegaChatSession
     */
    fun updateRemoteAudioVideo(type: Int, session: MegaChatSession) {
        speakerUser?.let {
            when {
                it.peerId == session.peerid && it.clientId == session.clientid -> {
                    when (type) {
                        Constants.TYPE_VIDEO -> {
                            when {
                                it.isVideoOn -> {
                                    activateRemoteVideo(it)
                                }
                                else -> {
                                    showAvatar(it)
                                    checkSessionOnHold(it)
                                }
                            }
                        }
                        Constants.TYPE_AUDIO -> {
                            speaker_mute_icon.isVisible = !it.isAudioOn
                        }
                    }
                }
            }
        }
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            adapter.updateParticipantAudioVideo(type, it)
        }
    }

    /**
     * Check changes call on hold
     *
     * @param isCallOnHold True, if the call is on hold. False, otherwise
     */
    fun updateCallOnHold(isCallOnHold: Boolean) {
        speakerUser?.let {
            checkCallOnHold(it)
        }
        val iterator = participants.iterator()
        iterator.forEach {
            adapter.updateCallOnHold(it, isCallOnHold)
        }
    }

    /**
     * Check changes session on hold
     *
     * @param session MegaChatSession
     */
    fun updateSessionOnHold(session: MegaChatSession) {
        speakerUser?.let {
            if (it.peerId == session.peerid && it.clientId == session.peerid) {
                checkSessionOnHold(it)
            }
        }
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            adapter.updateSessionOnHold(it, session.isOnHold)
        }
    }

    /**
     * Check changes in resolution
     *
     * @param listPeers List of participants with changes
     */
    fun updateRes(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                adapter.updateParticipantRes(it)
            }
        }
    }

    /**
     * Check changes in name
     *
     * @param listPeers List of participants with changes
     */
    fun updateName(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                adapter.updateName(it)
            }
        }
    }

    /**
     * Check changes in privileges
     *
     * @param listPeers List of participants with changes
     */
    fun updatePrivileges(listPeers: MutableSet<Participant>) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
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