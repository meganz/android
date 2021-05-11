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
import mega.privacy.android.app.utils.LogUtil
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
        if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSameCall(it.callId)) {
            speakerUser?.let { currentSpeaker ->
                if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic) {
                    logDebug("Received local audio level")
                    closeVideo(currentSpeaker)
                    (parentFragment as InMeetingFragment).inMeetingViewModel.assignMeAsSpeaker()
                }
            }
        }
    }

    private val remoteAudioLevelObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSameCall(callAndSession.first)) {
                speakerUser?.let { currentSpeaker ->
                    if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic &&
                        (currentSpeaker.peerId != callAndSession.second.peerid ||
                                currentSpeaker.clientId != callAndSession.second.clientid)
                    ) {
                        logDebug("Received remote audio level")
                        closeVideo(currentSpeaker)

                        selectSpeaker(
                            callAndSession.second.peerid,
                            callAndSession.second.clientid
                        )
                    }
                }
            }
        }


    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it
        if (isFirsTime) {
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
            EventObserver { participantClicked ->
                logDebug("Clicked participant")
                if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic) {
                    (parentFragment as InMeetingFragment).inMeetingViewModel.setSpeakerSelection(
                        false
                    )
                } else {
                    speakerUser?.let { currentSpeaker ->
                        if (currentSpeaker.peerId == participantClicked.peerId && currentSpeaker.clientId == participantClicked.clientId) {
                            (parentFragment as InMeetingFragment).inMeetingViewModel.setSpeakerSelection(
                                true
                            )
                        }
                    }
                }

                speakerUser?.let { currentSpeaker ->
                    if (currentSpeaker.peerId == participantClicked.peerId && currentSpeaker.clientId == participantClicked.clientId) {
                        logDebug("Same participant")
                        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                            participantClicked.peerId,
                            participantClicked.clientId
                        )?.let {
                            adapter.updatePeerSelected(participantClicked)
                        }
                    } else {
                        logDebug("New speaker selected")
                        closeVideo(currentSpeaker)
                        selectSpeaker(participantClicked.peerId, participantClicked.clientId)
                    }
                }
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
        ) { participant ->
            participant?.let { newSpeaker ->
                speakerUser?.let {
                    if (it.isVideoOn) {
                        closeVideo(it)
                    }
                }

                logDebug("New speaker selected")
                speakerUser = newSpeaker
                speakerUser?.let {
                    updateSpeakerUser(it)
                }
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                if (speakerParticipant.isMe) {
                    logDebug("Changes in local video")
                    speakerParticipant.isVideoOn = it
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

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            speakerUser?.let { speakerParticipant ->
                if (speakerParticipant.isMe) {
                    logDebug("Changes in local audio")
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

        logDebug("Select speaker")
        if (listParticipants.isNotEmpty()) {
            updateSpeakerPeers(listParticipants)
        }
    }

    /**
     * Monitor changes when updating the speaker participant
     *
     * @param participant
     */
    private fun updateSpeakerUser(participant: Participant) {
        speakerUser?.let {currentSpeaker ->
            if (participant.peerId != currentSpeaker.peerId || participant.clientId != currentSpeaker.clientId) return

            logDebug("Update participant selected")
            currentSpeaker.avatar?.let {
                speaker_avatar_image.setImageBitmap(it)
            }
            updateAudioIcon(currentSpeaker)

            when {
                currentSpeaker.isVideoOn -> {
                    logDebug("Update participant selected checkVideoOn")
                    checkVideoOn(currentSpeaker)
                }
                else -> {
                    logDebug("Update participant selected videoOffUI")
                    videoOffUI(currentSpeaker)
                }
            }
        }

    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    private fun updateAudioIcon(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            logDebug("Update audio icon")
            speaker_mute_icon.isVisible = !it.isAudioOn
        }
    }

    /**
     * Show UI when video is off
     *
     * @param participant
     */
    private fun videoOffUI(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            logDebug("UI video off")
            showAvatar(it)
            closeVideo(it)
            checkOnHold(it)
        }
    }

    /**
     * Method to show the Avatar
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            logDebug("Show avatar")
            speaker_avatar_image.isVisible = true
        }
    }

    /**
     * Method to close Video
     *
     * @param participant
     */
    private fun closeVideo(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

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

    /**
     * Method to close local video
     *
     * @param participant
     */
    private fun closeLocalVideo(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId || it.videoListener == null) return

            logDebug("Remove local video listener")
            sharedModel.removeLocalVideo(
                (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                it.videoListener
            )

            it.videoListener = null
        }
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant
     */
    private fun checkOnHold(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            val isCallOnHold =
                (parentFragment as InMeetingFragment).inMeetingViewModel.isCallOnHold()

            if (it.isMe) {
                when {
                    isCallOnHold -> {
                        logDebug("Call is on hold")
                        speaker_on_hold_icon.isVisible = true
                        speaker_avatar_image.alpha = 0.5f
                    }
                    else -> {
                        logDebug("Call is in progress")
                        speaker_on_hold_icon.isVisible = false
                        speaker_avatar_image.alpha = 1f
                    }
                }
                return
            }

            val isSessionOnHold =
                (parentFragment as InMeetingFragment).inMeetingViewModel.isSessionOnHold(
                    it.clientId
                )
            if (isSessionOnHold) {
                logDebug("Session is on hold ")
                speaker_on_hold_icon.isVisible = true
                speaker_avatar_image.alpha = 0.5f
                return
            }

            logDebug("Session is in progress")
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

    /**
     * Control when a change is received in the video flag
     *
     * @param participant
     */
    private fun checkVideoOn(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            if (it.isVideoOn && ((it.isMe && !(parentFragment as InMeetingFragment).inMeetingViewModel.isCallOnHold()) ||
                        (!it.isMe && !(parentFragment as InMeetingFragment).inMeetingViewModel.isCallOrSessionOnHold(
                            it.clientId
                        )))
            ) {
                logDebug("Video should be on")
                videoOnUI(it)
                return
            }

            logDebug("Video should be off")
            videoOffUI(it)
        }
    }

    /**
     * Show UI when video is on
     *
     * @param participant
     */
    private fun videoOnUI(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            logDebug("UI video on")
            hideAvatar(it)
            activateVideo(it)
        }
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant
     */
    private fun hideAvatar(participant: Participant) {
        speakerUser?.let {
            if (participant.peerId != it.peerId || participant.clientId != it.clientId) return

            logDebug("Hide Avatar")
            speaker_on_hold_icon.isVisible = false
            speaker_avatar_image.alpha = 1f
            speaker_avatar_image.isVisible = false
        }
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateVideo(participant: Participant) {
        speakerUser?.let { currentSpeaker ->
            if (participant.peerId != currentSpeaker.peerId || participant.clientId != currentSpeaker.clientId) return

            closeVideo(participant)
            if (currentSpeaker.videoListener != null) {
                logDebug("Video listener is not null participant")
                currentSpeaker.videoListener!!.height = 0
                currentSpeaker.videoListener!!.width = 0
            } else {
                logDebug("Video listener is null")
                val vListener = MeetingVideoListener(
                    speaker_video,
                    MegaApplication.getInstance().applicationContext.displayMetrics,
                    currentSpeaker.clientId,
                    false
                )

                currentSpeaker.videoListener = vListener
                if (currentSpeaker.isMe) {
                    sharedModel.addLocalVideo(
                        (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                        vListener
                    )
                } else {
                    (parentFragment as InMeetingFragment).inMeetingViewModel.onActivateVideo(
                        currentSpeaker
                    )
                }
            }

            speaker_video.isVisible = true
        }
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded
     * @param position
     * @param participantList
     */
    fun peerAddedOrRemoved(
        isAdded:Boolean,
        position: Int,
        participantList: MutableList<Participant>){
        adapter.submitList(participantList)
        if(isAdded){
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
        }else{
            //Participant in list
//            for (i in position until participants.size) {
//                (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(participants[i])
//            }

            logDebug("Participant Removed - notify item removed")
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, participants.size)
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
     * Check changes in resolution
     *
     * @param isHiRes
     * @param session MegaChatSession
     */
    fun updateRemoteResolution(isHiRes: Boolean, session: MegaChatSession) {
        //Speaker
        if (isHiRes) {
            speakerUser?.let { speaker ->
                if (session.peerid == speaker.peerId && session.clientid == speaker.clientId && !speaker.isMe && speaker.isVideoOn) {
                    logDebug("Update resolution, close video and checkVideo ON")
                    closeVideo(speaker)
                    checkVideoOn(speaker)
                }
            }
        }

        //List
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            adapter.updateRemoteResolution(it)
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
            if (session.peerid == speaker.peerId && session.clientid == speaker.clientId && !speaker.isMe) {
                speaker.isAudioOn = session.hasAudio()
                speaker.isVideoOn = session.hasVideo()

                when (type) {
                    Constants.TYPE_VIDEO -> {
                        logDebug("Update remote A/V, checkVideo ON")
                        checkVideoOn(speaker)
                    }
                    Constants.TYPE_AUDIO -> {
                        logDebug("Update remote A/V, update audio")
                        updateAudioIcon(speaker)
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
        speakerUser?.let { speaker ->
            when {
                isCallOnHold -> {
                    logDebug("Call on hold")
                    videoOffUI(speaker)
                }
                else -> {
                    logDebug("Call in progress")
                    checkVideoOn(speaker)
                }
            }
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
            if (it.peerId == session.peerid && it.clientId == session.clientid) {
                when {
                    session.isOnHold -> {
                        logDebug("Session is on hold")
                        videoOffUI(it)
                    }
                    else -> {
                        logDebug("Session is in progress")
                        checkVideoOn(it)
                    }
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

    /**
     * Method to destroy the surfaceView.
     */
    private fun closeAllVideos() {
        speakerUser?.let {
            speaker_video?.let { surfaceView ->
                surfaceView.isVisible = false
            }

            when {
                it.isMe -> {
                    logDebug("Close local video")
                    closeLocalVideo(it)
                }
                else -> {
                    logDebug("Close remote video")
                    (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(
                        it
                    )
                }
            }
        }

        val iterator = participants.iterator()
        iterator.forEach {
            adapter.closeAllVideos(it)
        }
    }

    override fun onDestroyView() {
        logDebug("onDestroyView")
        closeAllVideos()
        super.onDestroyView()
    }

    override fun onDestroy() {
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
        super.onDestroy()
    }
}