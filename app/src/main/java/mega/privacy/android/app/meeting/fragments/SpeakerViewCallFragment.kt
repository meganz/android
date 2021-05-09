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
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession
import org.jetbrains.anko.displayMetrics

class SpeakerViewCallFragment : MeetingBaseFragment() {

    lateinit var adapter: VideoListViewAdapter
    private var participants: MutableList<Participant> = mutableListOf()
    private var speakerUser: Participant? = null

    private var isFirsTime = true

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
        if(isFirsTime){
            isFirsTime = false
            adapter.submitList(null)
            adapter.submitList(it)
        }
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

        participants_horizontal_list.recycledViewPool.clear()
        participants_horizontal_list.adapter = null
        adapter = VideoListViewAdapter(
            (parentFragment as InMeetingFragment).inMeetingViewModel,
            participants_horizontal_list
        )

        adapter.submitList(null)
        adapter.submitList(participants)
        participants_horizontal_list.adapter = adapter

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

//        // TODO test code start
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.observeForever(
            participantsObserver
        )
//        // TODO test code end

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
                speakerUser =
                    (parentFragment as InMeetingFragment).inMeetingViewModel.createSpeakerParticipant(
                        it
                    )
                updateSpeakerUser(it)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                when {
                    speakerParticipant.isMe -> {
                        if (speakerParticipant.isMe) {
                            speakerParticipant.isVideoOn = it
                        }
                        when {
                            it -> {
                                checkVideoOn(speakerParticipant)
                            }
                            else -> {
                                videoOffUI(speakerParticipant)
                            }
                        }
                    }
                }
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                if (speakerParticipant.isMe) {
                    speakerParticipant.isAudioOn = it
                    updateAudioIcon(speakerParticipant)
                }
            }
        }
    }

    /**
     * Method for selecting a new speaker
     *
     * @param peerId
     * @param clientId
     */
    private fun selectSpeaker(peerId: Long, clientId: Long) {
        val listParticipants =
            (parentFragment as InMeetingFragment).inMeetingViewModel.updatePeerSelected(
                peerId, clientId
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
        speakerUser?.let {
            it.avatar?.let {
                speaker_avatar_image.setImageBitmap(it)
            }
            updateAudioIcon(it)

            when {
                participant.isVideoOn -> {
                    checkVideoOn(it)
                }
                else -> {
                    videoOffUI(it)
                }
            }
        }

    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    fun updateAudioIcon(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("Update audio icon")
                    speaker_mute_icon.isVisible = !it.isAudioOn
                }
            }
        }
    }

    /**
     * Show UI when video is off
     *
     * @param participant
     */
    fun videoOffUI(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("UI video off")
                    showAvatar(it)
                    closeVideo(it)
                    checkOnHold(it)
                }
            }
        }
    }

    /**
     * Method to show the Avatar
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("Show avatar")
                    speaker_avatar_image.isVisible = true
                }
            }
        }
    }

    /**
     * Method to close Video
     *
     * @param participant
     */
    private fun closeVideo(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("Close video")
                    speaker_video.isVisible = false
                    when {
                        it.isMe -> {
                            closeLocalVideo(it)
                        }
                        else -> {
                            (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(
                                it
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to close local video
     *
     * @param participant
     */
    private fun closeLocalVideo(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    when (it.videoListener) {
                        null -> return
                        else -> {
                            logDebug("Remove local video listener")
                            sharedModel.removeLocalVideo(
                                (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                                it.videoListener
                            )

                            it.videoListener = null
                        }
                    }
                }
            }
        }
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant
     */
    private fun checkOnHold(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    val isCallOnHold =
                        (parentFragment as InMeetingFragment).inMeetingViewModel.isCallOnHold()
                    when {
                        it.isMe -> {
                            when {
                                isCallOnHold -> {
                                    speaker_on_hold_icon.isVisible = true
                                    speaker_avatar_image.alpha = 0.5f
                                }
                                else -> {
                                    speaker_on_hold_icon.isVisible = false
                                    speaker_avatar_image.alpha = 1f
                                }
                            }
                        }
                        else -> {
                            val isSessionOnHold =
                                (parentFragment as InMeetingFragment).inMeetingViewModel.isSessionOnHold(
                                    it.clientId
                                )
                            when {
                                isSessionOnHold -> {
                                    logDebug("Show on hold icon participant ")
                                    speaker_on_hold_icon.isVisible = true
                                    speaker_avatar_image.alpha = 0.5f
                                }
                                else -> {
                                    logDebug("Hide on hold icon")
                                    speaker_on_hold_icon.isVisible = false
                                    when {
                                        isCallOnHold -> {
                                            speaker_avatar_image.alpha = 0.5f
                                        }
                                        else -> {
                                            speaker_avatar_image.alpha = 1f
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param participant
     */
    fun checkVideoOn(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    when {
                        it.isVideoOn && ((it.isMe && !(parentFragment as InMeetingFragment).inMeetingViewModel.isCallOnHold()) ||
                                (!it.isMe && !(parentFragment as InMeetingFragment).inMeetingViewModel.isCallOrSessionOnHold(
                                    it.clientId
                                ))) -> {
                            logDebug("Video should be on")
                            videoOnUI(it)
                        }
                        else -> {
                            logDebug("Video should be off")
                            videoOffUI(it)
                        }
                    }
                }
            }
        }
    }

    /**
     * Show UI when video is on
     *
     * @param participant
     */
    private fun videoOnUI(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("UI video on")
                    hideAvatar(it)
                    activateVideo(it)
                }
            }
        }
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant
     */
    private fun hideAvatar(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    logDebug("Hide Avatar")
                    speaker_on_hold_icon.isVisible = false
                    speaker_avatar_image.alpha = 1f
                    speaker_avatar_image.isVisible = false
                }
            }
        }
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateVideo(participant: Participant) {
        speakerUser?.let {
            when {
                participant.peerId != it.peerId || participant.clientId != it.clientId -> return
                else -> {
                    closeVideo(it)

                    when (it.videoListener) {
                        null -> {
                            logDebug("Video Listener is null ")
                            val vListener = MeetingVideoListener(
                                speaker_video,
                                MegaApplication.getInstance().applicationContext.displayMetrics,
                                it.clientId,
                                false
                            )

                            it.videoListener = vListener
                            when {
                                participant.isMe -> {
                                    sharedModel.addLocalVideo(
                                        (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                                        vListener
                                    )

                                }
                                else -> {
                                    (parentFragment as InMeetingFragment).inMeetingViewModel.onActivateVideo(
                                        it
                                    )
                                }
                            }

                        }
                        else -> {
                            logDebug("Video Listener is not null ")
                            participant.videoListener!!.height = 0
                            participant.videoListener!!.width = 0
                        }
                    }

                    speaker_video.isVisible = true
                }
            }
        }
    }

    /**
     * Update the adapter if a participant joins or leaves the call
     *
     * @param isAdded if is aader or removed
     * @param position
     */
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
        participantList: MutableList<Participant>
    ) {
        adapter.submitList(participantList)
        when {
            isAdded -> {
                when (position) {
                    0 -> {
                        logDebug("Participant added - notify data set changed")
                        adapter.notifyDataSetChanged()
                    }
                    else -> {
                        logDebug("Participant added - notify item inserted in $position")
                        adapter.notifyItemInserted(position)
                    }
                }
            }
            else -> {
                logDebug("Participant added - notify item removed in $position")
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
        //Speaker
        speakerUser?.let { speaker ->
            when {
                session.peerid == speaker.peerId && session.clientid == speaker.clientId && !speaker.isMe -> {
                    speaker.isAudioOn = session.hasAudio()
                    speaker.isVideoOn = session.hasVideo()
                    when (type) {
                        Constants.TYPE_VIDEO -> {
                            checkVideoOn(speaker)
                        }
                        Constants.TYPE_AUDIO -> {
                            updateAudioIcon(speaker)
                        }
                    }
                }
            }
        }

        //Participant in list
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
        //Speaker
        speakerUser?.let {
            logDebug("Session is on hold")
            videoOffUI(it)
        }

        //Participant in list
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
        //Speaker
        speakerUser?.let {
            when {
                it.peerId == session.peerid && it.clientId == session.clientid -> {
                    logDebug("Session is on hold")
                    videoOffUI(it)
                }
            }
        }

        //Participant in list
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

    companion object {

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }
}