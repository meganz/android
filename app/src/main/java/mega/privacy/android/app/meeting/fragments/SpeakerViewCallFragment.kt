package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession

class SpeakerViewCallFragment : MeetingBaseFragment(),
    MegaSurfaceRenderer.MegaSurfaceRendererListener {

    lateinit var adapter: VideoListViewAdapter
    private var participants: MutableList<Participant> = mutableListOf()
    private var speakerUser: Participant? = null

    private var isFirsTime = true

    private val localAudioLevelObserver = Observer<MegaChatCall> {
        if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSameCall(it.callId)) {
            speakerUser?.let {
                if ((parentFragment as InMeetingFragment).inMeetingViewModel.isSpeakerSelectionAutomatic) {
                    logDebug("Received local audio level")
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

                        logDebug("Received remote audio level with clientId ${callAndSession.second.clientid}")
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
            logDebug("Participants changed")
            isFirsTime = false
            adapter.submitList(null)
            adapter.submitList(participants)
        }

        updateVisibleParticipantsSpeakerView(participants)
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
        participants_horizontal_list.recycledViewPool.clear()

        adapter = VideoListViewAdapter(
            (parentFragment as InMeetingFragment).inMeetingViewModel,
            participants_horizontal_list,
            this
        )

        logDebug("View created and participants added")
        adapter.submitList(null)
        adapter.submitList(participants)
        updateVisibleParticipantsSpeakerView(participants)
        participants_horizontal_list.adapter = adapter

        (parentFragment as InMeetingFragment).inMeetingViewModel.pinItemEvent.observe(
            viewLifecycleOwner,
            EventObserver { participantClicked ->
                logDebug("Clicked in participant with clientId ${participantClicked.clientId}")
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
                        logDebug("Same participant, clientId ${currentSpeaker.clientId}")
                        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
                            currentSpeaker.peerId,
                            currentSpeaker.clientId
                        )?.let {
                            adapter.updatePeerSelected(currentSpeaker)
                        }
                    } else {
                        logDebug("New speaker selected with clientId ${currentSpeaker.clientId}")
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
                        logDebug("Close video of last speaker, is ${it.clientId}")
                        closeVideo(it)
                    }
                }

                logDebug("New speaker selected")
                speakerUser = newSpeaker
                speakerUser?.let {
                    logDebug("Update new speaker selected with clientId ${it.clientId}")
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
                        it -> checkVideoOn(speakerParticipant)
                        else -> videoOffUI(speakerParticipant)
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
     * Method that updates the number of participants visible on the recyclerview
     */
    private fun updateVisibleParticipantsSpeakerView(lis: List<Participant>) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.updateVisibleParticipants(
            lis
        )
    }

    /**
     * Method for selecting a new speaker
     *
     * @param peerId
     * @param clientId
     */
    private fun selectSpeaker(peerId: Long, clientId: Long) {
        logDebug("Selected new speaker with clientId $clientId")
        val listParticipants =
            (parentFragment as InMeetingFragment).inMeetingViewModel.updatePeerSelected(
                peerId, clientId
            )

        if (listParticipants.isNotEmpty()) {
            logDebug("Update the rest of participants")
            updateSpeakerPeers(listParticipants)
        }
    }

    /**
     * Monitor changes when updating the speaker participant
     *
     * @param participant
     */
    private fun updateSpeakerUser(participant: Participant) {
        speakerUser?.let { currentSpeaker ->
            if (participant.peerId != currentSpeaker.peerId || participant.clientId != currentSpeaker.clientId) return

            currentSpeaker.avatar?.let {
                speaker_avatar_image.setImageBitmap(it)
            }
            updateAudioIcon(currentSpeaker)

            when {
                currentSpeaker.isVideoOn -> checkVideoOn(currentSpeaker)
                else -> videoOffUI(currentSpeaker)
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

            logDebug("Update audio icon, clientId ${it.clientId}")
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

            logDebug("UI video off, speaker with clientId ${it.clientId}")
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
        speakerUser?.let { speaker ->
            if (participant.peerId != speaker.peerId || participant.clientId != speaker.clientId) return

            parent_surface_view.isVisible = false
            when {
                speaker.isMe -> {
                    logDebug("Close local video")
                    closeLocalVideo(speaker)
                }
                else -> {
                    logDebug("Close remote video")
                    (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(
                        speaker
                    )
                }
            }

            speaker.videoListener?.let { listener ->
                logDebug("Removing texture view")
                if (parent_surface_view.childCount > 0) {
                    parent_surface_view.removeAllViews()
                }

                listener.textureView?.let { view ->
                    view.parent?.let { viewParent ->
                        (viewParent as ViewGroup).removeView(view)
                    }
                }

                speaker.videoListener = null
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
            (parentFragment as InMeetingFragment).inMeetingViewModel.removeLocalVideoSpeaker(
                (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                it.videoListener
            )
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

            logDebug("UI video on, speaker with clientId ${it.clientId}")
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

            /*Video*/
            if (currentSpeaker.videoListener == null) {
                logDebug("Active video when listener is null, clientId ${currentSpeaker.clientId}")
                parent_surface_view.removeAllViews()

                val myTexture = TextureView(MegaApplication.getInstance().applicationContext)
                myTexture.layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                myTexture.alpha = 1.0f
                myTexture.rotation = 0f

                val vListener = GroupVideoListener(
                    myTexture,
                    currentSpeaker.peerId,
                    currentSpeaker.clientId,
                    currentSpeaker.isMe
                )

                currentSpeaker.videoListener = vListener

                parent_surface_view.addView(currentSpeaker.videoListener!!.textureView)

                if (currentSpeaker.isMe) {
                    (parentFragment as InMeetingFragment).inMeetingViewModel.addLocalVideoSpeaker(
                        (parentFragment as InMeetingFragment).inMeetingViewModel.getChatId(),
                        vListener
                    )
                } else {
                    (parentFragment as InMeetingFragment).inMeetingViewModel.onActivateVideo(
                        currentSpeaker, true
                    )
                }
            } else {
                logDebug("Active video when listener is not null, clientId ${currentSpeaker.clientId}")
                if (parent_surface_view.childCount > 0) {
                    parent_surface_view.removeAllViews()
                }

                currentSpeaker.videoListener?.textureView?.let { textureView ->
                    textureView.parent?.let { textureViewParent ->
                        (textureViewParent as ViewGroup).removeView(textureView)
                    }
                }

                parent_surface_view.addView(currentSpeaker.videoListener?.textureView)

                currentSpeaker.videoListener?.height = 0
                currentSpeaker.videoListener?.width = 0
            }

            parent_surface_view.isVisible = true
        }
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded True, if the participant has joined. False, if the participant has left
     * @param position The position of the change
     */
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
    ) {

        adapter.submitList(participants) {
            logDebug("List updated " + adapter.currentList.size)
            if (isAdded) {
                logDebug("Participant added in $position")
                when (position) {
                    0 -> {
                        adapter.notifyDataSetChanged()
                    }
                    else -> {
                        adapter.notifyItemInserted(position)
                    }
                }
            } else {
                logDebug("Participant Removed in $position")
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, adapter.currentList.size)
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
                logDebug("Update the peer selected")
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
    fun updateRemoteResolutionOfSpeaker(isHiRes: Boolean, session: MegaChatSession) {
        if (!isHiRes) {
            logDebug("The speaker should have high resolution")
            return
        }
        //Speaker
        speakerUser?.let { speaker ->
            if (session.peerid == speaker.peerId && session.clientid == speaker.clientId && !speaker.isMe && speaker.isVideoOn) {
                logDebug("Update speaker resolution (HiRes)")
                (parentFragment as InMeetingFragment).inMeetingViewModel.onActivateVideo(
                    speaker, true
                )
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
            if (session.peerid == speaker.peerId && session.clientid == speaker.clientId && !speaker.isMe) {
                speaker.isAudioOn = session.hasAudio()
                speaker.isVideoOn = session.hasVideo()

                when (type) {
                    Constants.TYPE_VIDEO -> {
                        logDebug("Update speaker video")
                        checkVideoOn(speaker)
                    }
                    Constants.TYPE_AUDIO -> {
                        logDebug("Update speaker audio")
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
            logDebug("Update remote A/V")
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
                    logDebug("Speaker call is on hold")
                    videoOffUI(speaker)
                }
                else -> {
                    logDebug("Speaker call is in progress")
                    checkVideoOn(speaker)
                }
            }
        }

        //Participant in list
        val iterator = participants.iterator()
        iterator.forEach {
            logDebug("Update call on hold status")
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
                        logDebug("Speaker session is on hold")
                        videoOffUI(it)
                    }
                    else -> {
                        logDebug("Speaker session is in progress")
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
            logDebug("Update session on hold status")
            adapter.updateSessionOnHold(it, session.isOnHold)
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
                logDebug("Update participant name")
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
                logDebug("Update participant privileges")
                adapter.updateParticipantPrivileges(it)
            }
        }
    }

    private fun resetSizeListener(participant: Participant) {
        if (!participant.isVideoOn)
            return

        participant.videoListener?.let {
            logDebug("Reset Size participant listener")
            it.height = 0
            it.width = 0
        }
    }

    companion object {

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }

    override fun onResume() {
        val iterator = participants.iterator()
        iterator.forEach { participant ->
            resetSizeListener(participant)
        }

        super.onResume()
    }

    /**
     * Method for resizing the listener
     *
     * @param peerId
     * @param clientId
     */
    override fun resetSize(peerId: Long, clientId: Long) {
        (parentFragment as InMeetingFragment).inMeetingViewModel.getParticipant(
            peerId,
            clientId
        )?.let {
            resetSizeListener(it)
        }
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    private fun removeTextureView() {
        speakerUser?.let {
            (parentFragment as InMeetingFragment).inMeetingViewModel.removeSelected(
                it.peerId,
                it.clientId
            )

            if (it.isMe) {
                logDebug("Close local video")
                closeLocalVideo(it)
            } else {
                logDebug("Close remote video")
                (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(
                    it
                )
            }

            logDebug("Remove texture view")
            if (parent_surface_view.childCount > 0) {
                parent_surface_view.removeAllViews()
                parent_surface_view.removeAllViewsInLayout()
            }

            it.videoListener?.let { listener ->
                listener.textureView?.let { textureView ->
                    textureView.parent?.let { surfaceParent ->
                        (surfaceParent as ViewGroup).removeView(textureView)
                    }
                    textureView.isVisible = false
                }

                it.videoListener = null
            }
        }

        val iterator = participants.iterator()
        iterator.forEach {
            (parentFragment as InMeetingFragment).inMeetingViewModel.onCloseVideo(it)
            logDebug("Remove texture view")
            adapter.removeTextureView(it)
        }
    }

    override fun onDestroyView() {
        logDebug("View destroyed")
        removeTextureView()
        super.onDestroyView()
    }

    override fun onDestroy() {
        (parentFragment as InMeetingFragment).inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
        super.onDestroy()
    }
}