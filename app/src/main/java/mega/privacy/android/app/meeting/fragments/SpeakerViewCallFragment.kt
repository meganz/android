package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.Pair
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.databinding.SpeakerViewCallFragmentBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatSession

class SpeakerViewCallFragment : MeetingBaseFragment(),
    MegaSurfaceRenderer.MegaSurfaceRendererListener {

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private lateinit var surfaceContainer: RelativeLayout

    private lateinit var speakerAvatar: RoundedImageView

    private lateinit var speakerOnHoldIcon: ImageView

    private lateinit var speakerMuteIcon: ImageView

    private lateinit var listView: RecyclerView

    private lateinit var adapter: VideoListViewAdapter

    private var participants: MutableList<Participant> = mutableListOf()

    private var speakerUser: Participant? = null

    private var isFirsTime = true

    private val remoteAudioLevelObserver = Observer<Pair<Long, MegaChatSession>> { callAndSession ->
        val callId = callAndSession.first
        val session = callAndSession.second

        if (inMeetingViewModel.isSameCall(callId)) {
            speakerUser?.let {
                if (inMeetingViewModel.isSpeakerSelectionAutomatic && (it.peerId != session.peerid || it.clientId != session.clientid)) {
                    logDebug("Received remote audio level with clientId ${session.clientid}")
                    selectSpeaker(
                        session.peerid,
                        session.clientid
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
            updateVisibleParticipantsSpeakerView(participants)
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inMeetingViewModel = (parentFragment as InMeetingFragment).inMeetingViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dataBinding = SpeakerViewCallFragmentBinding.inflate(inflater, container, false)

        listView = dataBinding.participantsHorizontalList
        speakerAvatar = dataBinding.speakerAvatarImage
        speakerOnHoldIcon = dataBinding.speakerOnHoldIcon
        speakerMuteIcon = dataBinding.speakerMuteIcon
        surfaceContainer = dataBinding.parentSurfaceView

        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val lm = LinearLayoutManager(context)
        lm.orientation = LinearLayoutManager.HORIZONTAL

        listView.apply {
            layoutManager = lm
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            clipToPadding = true
            setHasFixedSize(true)
            adapter = null
            recycledViewPool.clear()
        }

        adapter = VideoListViewAdapter(
            inMeetingViewModel,
            listView,
            this
        )

        logDebug("View created and participants added")
        adapter.submitList(null)
        adapter.submitList(participants)
        updateVisibleParticipantsSpeakerView(participants)
        listView.adapter = adapter

        observeViewModel()
        initLiveEventBus()

        inMeetingViewModel.getFirstParticipant(MEGACHAT_INVALID_HANDLE, MEGACHAT_INVALID_HANDLE)
            ?.let {
                selectSpeaker(it.peerId, it.clientId)
            }
    }

    private fun initLiveEventBus() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AUDIO_LEVEL_CHANGE)
            .observeSticky(this, remoteAudioLevelObserver as Observer<Any>)
    }

    private fun observeViewModel() {
        inMeetingViewModel.participants.observe(viewLifecycleOwner, participantsObserver)

        inMeetingViewModel.pinItemEvent.observe(
            viewLifecycleOwner,
            EventObserver { participantClicked ->
                logDebug("Clicked in participant with clientId ${participantClicked.clientId}")

                if (inMeetingViewModel.isSpeakerSelectionAutomatic) {
                    inMeetingViewModel.setSpeakerSelection(false)
                } else {
                    speakerUser?.let {
                        if (it.peerId == participantClicked.peerId && it.clientId == participantClicked.clientId) {
                            inMeetingViewModel.setSpeakerSelection(true)
                        }
                    }
                }

                if (speakerUser != null && speakerUser!!.peerId == participantClicked.peerId && speakerUser!!.clientId == participantClicked.clientId) {
                    if (participantClicked.clientId == MEGACHAT_INVALID_HANDLE) {
                        logDebug("Same participant(Me), clientId ${speakerUser!!.clientId}")
                    } else {
                        logDebug("Same participant, clientId ${speakerUser!!.clientId}")
                        adapter.updatePeerSelected(speakerUser!!)
                    }
                } else {
                    logDebug("New speaker selected with clientId ${participantClicked.clientId}")
                    selectSpeaker(participantClicked.peerId, participantClicked.clientId)
                }
            })

        inMeetingViewModel.speakerParticipant.observe(
            viewLifecycleOwner
        ) {
            speakerUser?.let { currentSpeaker ->
                if (currentSpeaker.isVideoOn) {
                    currentSpeaker.videoListener?.let { listener ->
                        logDebug("Close video of last speaker, is ${currentSpeaker.clientId}")
                        inMeetingViewModel.removeResolutionAndListener(
                            currentSpeaker,
                            listener
                        )
                        removeSpeakerListener()
                    }
                }
            }

            if (it == null) {
                logDebug("No speaker. The first on the list will be the new speaker")
                if (speakerUser != null) {
                    inMeetingViewModel.getFirstParticipant(
                        speakerUser!!.peerId,
                        speakerUser!!.clientId
                    )?.let { firstParticipant ->
                        selectSpeaker(firstParticipant.peerId, firstParticipant.clientId)
                    }
                } else {
                    inMeetingViewModel.getFirstParticipant(
                        MEGACHAT_INVALID_HANDLE,
                        MEGACHAT_INVALID_HANDLE
                    )?.let { firstParticipant ->
                        selectSpeaker(firstParticipant.peerId, firstParticipant.clientId)
                    }
                }
            } else {
                logDebug("Update new speaker selected with clientId ${it.clientId}")
                speakerUser = it
                updateSpeakerUser()
            }
        }
    }

    /**
     * Method that updates the number of participants visible on the recyclerview
     *
     * @param list list of participants
     */
    private fun updateVisibleParticipantsSpeakerView(list: List<Participant>) =
        inMeetingViewModel.updateVisibleParticipants(list)

    /**
     * Method for selecting a new speaker
     *
     * @param peerId User handle of a new speaker
     * @param clientId Client ID of a speaker
     */
    private fun selectSpeaker(peerId: Long, clientId: Long) {
        if (clientId == MEGACHAT_INVALID_HANDLE) return

        logDebug("Selected new speaker with clientId $clientId")
        val listParticipants = inMeetingViewModel.updatePeerSelected(peerId, clientId)
        if (listParticipants.isNotEmpty()) {
            logDebug("Update the rest of participants")
            updateSpeakerPeers(listParticipants)
        }
    }

    /**
     * Monitor changes when updating the speaker participant
     */
    private fun updateSpeakerUser() {
        speakerUser?.let {
            it.avatar?.let { bitmap ->
                speakerAvatar.setImageBitmap(bitmap)
            }

            updateAudioIcon(it)

            if (it.isVideoOn) {
                checkVideoOn(it)
            } else {
                videoOffUI(it)
            }
        }
    }

    /**
     * Check if mute icon should be visible
     *
     * @param speaker The current participant selected as speaker
     */
    private fun updateAudioIcon(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        logDebug("Update audio icon, clientId ${speaker.clientId}")
        speakerMuteIcon.isVisible = !speaker.isAudioOn
    }

    /**
     * Show UI when video is off
     *
     * @param speaker The current participant selected as speaker
     */
    private fun videoOffUI(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        logDebug("UI video off, speaker with clientId ${speaker.clientId}")
        showAvatar(speaker)
        closeVideo(speaker)
        checkOnHold(speaker)
    }

    /**
     * Method to show the Avatar
     *
     * @param speaker The current participant selected as speaker
     */
    private fun showAvatar(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        logDebug("Show avatar")
        speakerAvatar.isVisible = true
    }

    /**
     * Method to compare a participant with the current speaker
     *
     * @param toCheck Participant to compare
     * @return True, if different participant. False, if it is the same
     */
    private fun isSpeakerInvalid(toCheck: Participant): Boolean =
        if (speakerUser == null) true
        else toCheck.peerId != speakerUser!!.peerId || toCheck.clientId != speakerUser!!.clientId

    /**
     * Method to close Video
     *
     * @param speaker The current participant selected as speaker
     */
    private fun closeVideo(speaker: Participant) {
        if (isSpeakerInvalid(speaker) || speaker.videoListener == null) return

        surfaceContainer.isVisible = false

        logDebug("Close remote video")
        inMeetingViewModel.removeRemoteVideoResolution(speaker)
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param speaker The current participant selected as speaker
     */
    private fun checkOnHold(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        val isCallOnHold = inMeetingViewModel.isCallOnHold()
        val isSessionOnHold = inMeetingViewModel.isSessionOnHold(speaker.clientId)
        if (isSessionOnHold) {
            logDebug("Session is on hold ")
            speakerOnHoldIcon.isVisible = true
            speakerAvatar.alpha = 0.5f
            return
        }

        logDebug("Session is in progress")
        speakerOnHoldIcon.isVisible = false
        speakerAvatar.alpha = if (isCallOnHold) 0.5f else 1f
    }

    /**
     * Control when a change is received in the video flag
     *
     * @param speaker The current participant selected as speaker
     */
    private fun checkVideoOn(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        if (speaker.isVideoOn && (!inMeetingViewModel.isCallOrSessionOnHold(
                speaker.clientId
            ))
        ) {
            logDebug("Video should be on")
            videoOnUI(speaker)
            return
        }

        logDebug("Video should be off")
        videoOffUI(speaker)
    }

    /**
     * Show UI when video is on
     *
     * @param speaker The current participant selected as speaker
     */
    private fun videoOnUI(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        logDebug("UI video on, speaker with clientId ${speaker.clientId}")
        hideAvatar(speaker)
        activateVideo(speaker)
    }

    /**
     * Method to hide the Avatar
     *
     * @param speaker The current participant selected as speaker
     */
    private fun hideAvatar(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        logDebug("Hide Avatar")
        speakerOnHoldIcon.isVisible = false
        speakerAvatar.alpha = 1f
        speakerAvatar.isVisible = false
    }

    /**
     * Method for adding the speaker video listener
     *
     * @param speaker The speaker participant
     */
    private fun addSpeakerVideoListener(speaker: Participant) {
        if (speaker.isVideoOn && !inMeetingViewModel.isCallOrSessionOnHold(speaker.clientId)) {
            if (speaker.videoListener == null) {
                speaker.videoListener =
                    inMeetingViewModel.createVideoListener(
                        speaker,
                        AVATAR_VIDEO_VISIBLE,
                        ROTATION
                    )

                speaker.videoListener?.let { listener ->
                    surfaceContainer.addView(listener.textureView)
                }
            }

            speaker.videoListener?.let { listener ->
                logDebug("Add speaker video listener clientID ${speaker.clientId}")
                inMeetingViewModel.addChatRemoteVideoListener(
                    listener,
                    speaker.clientId,
                    inMeetingViewModel.getChatId(),
                    true
                )
            }
        }
    }

    /**
     * Method to add or remove video listener
     *
     * @param peerId Peer ID of the participant whose listener of the video is to be added or removed
     * @param clientId Client ID of the participant whose listener of the video is to be added or removed
     * @param shouldAddListener True, if the listener is to be added. False, if the listener should be removed
     */
    fun updateListenerSpeaker(peerId: Long, clientId: Long, shouldAddListener: Boolean) {
        speakerUser?.let {
            if (it.peerId != peerId || it.clientId != clientId) return
            if (shouldAddListener) {
                addSpeakerVideoListener(it)
            } else {
                it.videoListener?.let { listener ->
                    logDebug("Remove speaker video listener clientID ${it.clientId}")
                    inMeetingViewModel.removeChatRemoteVideoListener(
                        listener,
                        it.clientId,
                        inMeetingViewModel.getChatId(),
                        it.hasHiRes
                    )

                    removeSpeakerListener()
                }
            }
        }
    }

    /**
     * Method for activating the video
     *
     * @param speaker The current participant selected as speaker
     */
    private fun activateVideo(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        speakerUser?.let {
            /*Video*/
            if (it.videoListener == null) {
                logDebug("Active video when listener is null, clientId ${it.clientId}")
                surfaceContainer.removeAllViews()

                it.videoListener =
                    inMeetingViewModel.createVideoListener(it, AVATAR_VIDEO_VISIBLE, ROTATION)

                it.videoListener?.let { listener ->
                    surfaceContainer.addView(listener.textureView)
                }

                inMeetingViewModel.getSession(it.clientId)?.let { session ->
                    if (it.hasHiRes) {
                        if (!session.canRecvVideoHiRes()) {
                            logDebug("Asking for HiRes video, clientId ${it.clientId}")
                            inMeetingViewModel.requestHiResVideo(
                                session,
                                inMeetingViewModel.currentChatId
                            )
                        } else {
                            logDebug("Already have LowRes/HiRes video, clientId ${it.clientId}")
                            updateListenerSpeaker(it.peerId, it.clientId, true)
                        }
                    }
                }
            } else {
                logDebug("Active video when listener is not null, clientId ${it.clientId}")
                if (surfaceContainer.childCount > 0) {
                    surfaceContainer.removeAllViews()
                }

                it.videoListener?.textureView?.let { textureView ->
                    textureView.parent?.let { textureViewParent ->
                        (textureViewParent as ViewGroup).removeView(textureView)
                    }
                }

                surfaceContainer.addView(it.videoListener?.textureView)

                it.videoListener?.height = 0
                it.videoListener?.width = 0
            }

            surfaceContainer.isVisible = true
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
                if (position == 0) {
                    adapter.notifyDataSetChanged()
                } else {
                    adapter.notifyItemInserted(position)
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
        listPeers.forEach { peer ->
            inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                logDebug("Update the peer selected")
                adapter.updatePeerSelected(it)
            }
        }
    }

    /**
     * Check changes in remote A/V flags
     *
     * @param type type of change, Audio or Video
     * @param session MegaChatSession of a participant
     */
    fun updateRemoteAudioVideo(type: Int, session: MegaChatSession) {
        //Speaker
        speakerUser?.let { speaker ->
            if (session.peerid == speaker.peerId && session.clientid == speaker.clientId) {

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
        inMeetingViewModel.getParticipant(
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
            if (isCallOnHold) {
                logDebug("Speaker call is on hold")
                videoOffUI(speaker)
            } else {
                logDebug("Speaker call is in progress")
                checkVideoOn(speaker)
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
     * @param session MegaChatSession of a participant
     */
    fun updateSessionOnHold(session: MegaChatSession) {
        //Speaker
        speakerUser?.let {
            if (it.peerId == session.peerid && it.clientId == session.clientid) {
                if (session.isOnHold) {
                    logDebug("Speaker session is on hold")
                    videoOffUI(it)
                } else {
                    logDebug("Speaker session is in progress")
                    checkVideoOn(it)
                }
            }
        }

        //Participant in list
        inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            logDebug("Update session on hold status")
            adapter.updateSessionOnHold(it, session.isOnHold)
        }
    }

    /**
     * Method to control when the video listener should be added or removed.
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     */
    fun updateListener(participant: Participant, shouldAddListener: Boolean, isHiRes: Boolean) {
        //Speaker
        speakerUser?.let {
            if (it.peerId == participant.peerId && it.clientId == participant.clientId && isHiRes) {
                logDebug("Update listener in speaker, should the listener be added? $shouldAddListener")
                updateListenerSpeaker(it.peerId, it.clientId, shouldAddListener)
            }
        }

        //List of participants
        logDebug("Update listener in participants list, should the listener be added? $shouldAddListener")
        adapter.updateListener(participant, shouldAddListener, isHiRes)
    }

    /**
     * Check changes in name
     *
     * @param listPeers List of participants with changes
     * @param typeChange the type of change, name or avatar
     */
    fun updateNameOrAvatar(listPeers: MutableSet<Participant>, typeChange: Int) {
        val iterator = listPeers.iterator()
        iterator.forEach { peer ->
            inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                logDebug("Update participant name")
                adapter.updateNameOrAvatar(it, typeChange)
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
            inMeetingViewModel.getParticipant(
                peer.peerId,
                peer.clientId
            )?.let {
                logDebug("Update participant privileges")
                adapter.updateParticipantPrivileges(it)
            }
        }
    }

    companion object {
        const val AVATAR_VIDEO_VISIBLE = 1f
        const val ROTATION = 0f
        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }

    /**
     * Method for resizing the listener
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    override fun resetSize(peerId: Long, clientId: Long) {
        inMeetingViewModel.getParticipant(
            peerId,
            clientId
        )?.let {
            if (!it.isVideoOn || it.videoListener == null)
                return
            it.videoListener?.let { listener ->
                logDebug("Reset Size participant listener")
                listener.height = 0
                listener.width = 0
            }
        }
    }

    /**
     * Method for removing the video listener of Speaker
     */
    private fun removeSpeakerListener() {
        speakerUser?.let {
            logDebug("Remove texture view of speaker")
            if (surfaceContainer.childCount > 0) {
                surfaceContainer.removeAllViews()
            }

            it.videoListener?.let { listener ->
                listener.textureView?.let { textureView ->
                    textureView.parent?.let { surfaceParent ->
                        (surfaceParent as ViewGroup).removeView(textureView)
                    }
                    textureView.isVisible = false
                }
            }

            logDebug("Speaker ${it.clientId} video listener null")
            it.videoListener = null
        }
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    fun removeTextureView() {
        speakerUser?.let {
            inMeetingViewModel.removeSelected(
                it.peerId,
                it.clientId
            )

            it.videoListener?.let { listener ->
                inMeetingViewModel.removeResolutionAndListener(it, listener)
                removeSpeakerListener()
            }
        }

        val iterator = participants.iterator()
        iterator.forEach {
            logDebug("Remove texture view of participants")
            adapter.removeTextureView(it)
        }
    }

    override fun onDestroyView() {
        logDebug("View destroyed")
        removeTextureView()
        super.onDestroyView()
    }
}