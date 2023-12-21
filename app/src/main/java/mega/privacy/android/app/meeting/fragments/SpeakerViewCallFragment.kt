package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE
import mega.privacy.android.app.databinding.SpeakerViewCallFragmentBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.adapter.VideoListViewAdapter
import mega.privacy.android.app.presentation.meeting.view.SpeakerCallView
import mega.privacy.android.app.utils.Util
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.domain.entity.meeting.TypeRemoteAVFlagChange
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatSession
import timber.log.Timber

class SpeakerViewCallFragment : MeetingBaseFragment(),
    MegaSurfaceRenderer.MegaSurfaceRendererListener {

    private lateinit var inMeetingViewModel: InMeetingViewModel

    private lateinit var surfaceContainer: RelativeLayout

    private lateinit var speakerAvatar: RoundedImageView

    private lateinit var speakerOnHoldIcon: ImageView

    private lateinit var textViewName: EmojiTextView

    private lateinit var speakerMuteIcon: ImageView

    private lateinit var speakerSpeakingIcon: ImageView

    private lateinit var listView: RecyclerView

    private lateinit var adapter: VideoListViewAdapter

    private var participants: MutableList<Participant> = mutableListOf()

    private var isFirsTime = true

    private val remoteAudioLevelObserver = Observer<Pair<Long, MegaChatSession>> { callAndSession ->
        val callId = callAndSession.first
        val session = callAndSession.second

        if (inMeetingViewModel.isSameCall(callId) && inMeetingViewModel.state.value.isSpeakerSelectionAutomatic) {
            val currentSpeaker = inMeetingViewModel.getCurrentSpeakerParticipant()
            if (currentSpeaker == null || currentSpeaker.peerId != session.peerid || currentSpeaker.clientId != session.clientid) {
                Timber.d("Received remote audio level with clientId ${session.clientid}")
                selectSpeaker(
                    session.peerid,
                    session.clientid
                )
            } else {
                Timber.d("Received remote audio level with clientId ${session.clientid}, same current speaker")
            }
        }

        updateRemoteAudioVideo(TypeRemoteAVFlagChange.Audio, session)
    }

    private val participantsObserver = Observer<MutableList<Participant>> {
        participants = it

        if (isFirsTime) {
            Timber.d("Participants changed")
            isFirsTime = false
            adapter.submitList(null)
            adapter.submitList(participants)
        }
    }

    private val speakerParticipantsObserver = Observer<MutableList<Participant>> {
        val newSpeaker = inMeetingViewModel.getCurrentSpeakerParticipant()
        if (newSpeaker == null) {
            inMeetingViewModel.getFirstParticipant(
                MEGACHAT_INVALID_HANDLE,
                MEGACHAT_INVALID_HANDLE
            )?.apply {
                selectSpeaker(peerId, clientId)
                updateSpeakerTextViewName(name, isPresenting)
            }

        } else {
            inMeetingViewModel.getSession(newSpeaker.clientId)?.apply {
                if (!hasVideo() || inMeetingViewModel.isSessionOnHold(newSpeaker.clientId)) {
                    removeTextureViewOfPreviousSpeaker(newSpeaker.peerId, newSpeaker.clientId)
                    inMeetingViewModel.removePreviousSpeakers()
                }
            }
            updateSpeakerUser()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inMeetingViewModel = (parentFragment as InMeetingFragment).inMeetingViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val dataBinding = SpeakerViewCallFragmentBinding.inflate(inflater, container, false)

        listView = dataBinding.participantsHorizontalList
        speakerAvatar = dataBinding.speakerAvatarImage
        speakerOnHoldIcon = dataBinding.speakerOnHoldIcon
        textViewName = dataBinding.name
        speakerMuteIcon = dataBinding.speakerMuteIcon
        speakerSpeakingIcon = dataBinding.speakerSpeakingIcon
        surfaceContainer = dataBinding.parentSurfaceView
        dataBinding.snackbarComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by inMeetingViewModel.state.collectAsStateWithLifecycle()
                MegaAppTheme(isDark = true) {
                    SpeakerCallView(
                        state = state,
                        onSnackbarMessageConsumed = {
                            inMeetingViewModel.onSnackbarMessageConsumed()
                        },
                    )

                }
            }
        }

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

        Timber.d("View created and participants added")
        adapter.submitList(null)
        adapter.submitList(participants)
        listView.adapter = adapter

        observeViewModel()
        initLiveEventBus()

        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { currentSpeaker ->
            updateSpeakerTextViewName(currentSpeaker.name, currentSpeaker.isPresenting)
        } ?: {
            inMeetingViewModel.getFirstParticipant(MEGACHAT_INVALID_HANDLE, MEGACHAT_INVALID_HANDLE)
                ?.apply {
                    selectSpeaker(peerId, clientId)
                    updateSpeakerTextViewName(name, isPresenting)
                }
        }
    }

    /**
     * Update name in speaker view
     *
     * @param name Speaker name
     * @param isPresenting  True, if is presenting. False if not.
     */
    private fun updateSpeakerTextViewName(name: String, isPresenting: Boolean) {
        if (isPresenting) {
            textViewName.text = getString(
                R.string.meetings_meeting_screen_main_view_participant_is_presenting_label,
                name
            )
        } else {
            textViewName.text = name
        }
    }

    private fun initLiveEventBus() {
        LiveEventBus.get<Pair<Long, MegaChatSession>>(EVENT_REMOTE_AUDIO_LEVEL_CHANGE)
            .observeSticky(this, remoteAudioLevelObserver)
    }

    private fun observeViewModel() {
        inMeetingViewModel.participants.observe(viewLifecycleOwner, participantsObserver)
        inMeetingViewModel.speakerParticipants.observe(
            viewLifecycleOwner,
            speakerParticipantsObserver
        )

        inMeetingViewModel.pinItemEvent.observe(
            viewLifecycleOwner,
            EventObserver { participantClicked ->
                Timber.d("Clicked in participant with clientId ${participantClicked.clientId}")
                if (inMeetingViewModel.state.value.isSpeakerSelectionAutomatic) {
                    inMeetingViewModel.setSpeakerSelection(false)
                } else {
                    inMeetingViewModel.getCurrentSpeakerParticipant()?.let {
                        if (it.peerId == participantClicked.peerId && it.clientId == participantClicked.clientId) {
                            inMeetingViewModel.setSpeakerSelection(true)
                        }
                    }
                }

                inMeetingViewModel.getCurrentSpeakerParticipant()
                    ?.let { currentSpeakerParticipant ->
                        if (currentSpeakerParticipant.peerId == participantClicked.peerId && currentSpeakerParticipant.clientId == participantClicked.clientId) {
                            Timber.d(" Same participant, clientId ${currentSpeakerParticipant.clientId}")
                            updateSpeakerTextViewName(
                                currentSpeakerParticipant.name,
                                currentSpeakerParticipant.isPresenting
                            )
                            adapter.updatePeerSelected(currentSpeakerParticipant)
                            return@EventObserver
                        }
                    }

                Timber.d("New speaker selected with clientId ${participantClicked.clientId}")
                selectSpeaker(participantClicked.peerId, participantClicked.clientId)
                updateSpeakerTextViewName(
                    participantClicked.name,
                    participantClicked.isPresenting
                )
            })
    }

    /**
     * Method for selecting a new speaker
     *
     * @param peerId User handle of a new speaker
     * @param clientId Client ID of a speaker
     */
    private fun selectSpeaker(peerId: Long, clientId: Long) {
        if (clientId == MEGACHAT_INVALID_HANDLE) return

        val listParticipants = inMeetingViewModel.updatePeerSelected(peerId, clientId)
        if (listParticipants.isNotEmpty()) {
            Timber.d("Update the rest of participants")
            updateSpeakerPeers(listParticipants)
        }
        inMeetingViewModel.checkScreensShared()
    }

    /**
     * Monitor changes when updating the speaker participant
     */
    private fun updateSpeakerUser() {

        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            speaker.avatar?.let { bitmap ->
                speakerAvatar.setImageBitmap(bitmap)
            }
            updateAudioIcon(speaker)
            inMeetingViewModel.getSession(speaker.clientId)?.let {
                if (it.hasVideo()) {
                    checkVideoOn(speaker)
                } else {
                    videoOffUI(speaker)
                }
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

        Timber.d("Update audio icon, clientId ${speaker.clientId}")
        speakerMuteIcon.isVisible = !speaker.isAudioOn && !speaker.isPresenting
        speakerSpeakingIcon.isVisible = speaker.isAudioDetected && !speaker.isPresenting
    }

    /**
     * Show UI when video is off
     *
     * @param speaker The current participant selected as speaker
     */
    private fun videoOffUI(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        Timber.d("UI video off, speaker with clientId ${speaker.clientId}")
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

        Timber.d("Show avatar")
        speakerAvatar.isVisible = true
    }

    /**
     * Method to compare a participant with the current speaker
     *
     * @param toCheck Participant to compare
     * @return True, if different participant. False, if it is the same
     */
    private fun isSpeakerInvalid(toCheck: Participant): Boolean {
        val currentSpeakerParticipant =
            inMeetingViewModel.getCurrentSpeakerParticipant() ?: return true

        return toCheck.peerId != currentSpeakerParticipant.peerId || toCheck.clientId != currentSpeakerParticipant.clientId
    }

    /**
     * Method to close Video
     *
     * @param speaker The current participant selected as speaker
     */
    private fun closeVideo(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        surfaceContainer.isVisible = false

        if (speaker.videoListener != null) {
            Timber.d("Close remote video")
            inMeetingViewModel.removeRemoteVideoResolution(speaker)
        }
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
            Timber.d("Session is on hold ")
            speakerOnHoldIcon.isVisible = true
            speakerAvatar.alpha = 0.5f
            return
        }

        Timber.d("Session is in progress")
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

        inMeetingViewModel.getSession(speaker.clientId)?.let {
            if (it.hasVideo() && (!inMeetingViewModel.isCallOrSessionOnHold(
                    speaker.clientId
                ))
            ) {
                Timber.d("Video should be on")
                videoOnUI(speaker)
                return
            }
        }

        Timber.d("Video should be off")
        videoOffUI(speaker)
    }

    /**
     * Show UI when video is on
     *
     * @param speaker The current participant selected as speaker
     */
    private fun videoOnUI(speaker: Participant) {
        if (isSpeakerInvalid(speaker)) return

        Timber.d("UI video on, speaker with clientId ${speaker.clientId}")
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

        Timber.d("Hide Avatar")
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
        inMeetingViewModel.getSession(speaker.clientId)?.apply {
            if (hasVideo() && !inMeetingViewModel.isCallOrSessionOnHold(speaker.clientId)) {
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
                    Timber.d("Add speaker video listener clientID ${speaker.clientId}")
                    inMeetingViewModel.addChatRemoteVideoListener(
                        listener,
                        speaker.clientId,
                        inMeetingViewModel.getChatId(),
                        true
                    )
                }
            }
        }
    }

    /**
     * Method to remove from the textures view of the above speakers
     *
     * @param peerId Peer ID of the current speaker
     * @param clientId Client ID of the current speaker
     */
    private fun removeTextureViewOfPreviousSpeaker(peerId: Long, clientId: Long) {
        inMeetingViewModel.getPreviousSpeakers(peerId, clientId)?.let { list ->
            val iterator = list.iterator()
            iterator.forEach { peer ->
                peer.videoListener?.let { listener ->
                    surfaceContainer.removeView(listener.textureView)
                }
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
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { currentSpeaker ->
            if (currentSpeaker.peerId != peerId || currentSpeaker.clientId != clientId) return

            if (shouldAddListener) {
                addSpeakerVideoListener(currentSpeaker)
                removeTextureViewOfPreviousSpeaker(currentSpeaker.peerId, currentSpeaker.clientId)
                inMeetingViewModel.removePreviousSpeakers()

            } else {
                currentSpeaker.videoListener?.let { listener ->
                    Timber.d("Remove speaker video listener clientID ${currentSpeaker.clientId}")
                    inMeetingViewModel.removeChatRemoteVideoListener(
                        listener,
                        currentSpeaker.clientId,
                        inMeetingViewModel.getChatId(),
                        currentSpeaker.hasHiRes
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
        if (speaker.videoListener == null) {
            Timber.d("Active video when listener is null, clientId ${speaker.clientId}")
            speaker.videoListener =
                inMeetingViewModel.createVideoListener(speaker, AVATAR_VIDEO_VISIBLE, ROTATION)

            speaker.videoListener?.let { listener ->
                surfaceContainer.addView(listener.textureView)
            }

            inMeetingViewModel.getSession(speaker.clientId)?.let { session ->
                if (speaker.hasHiRes) {
                    if (!session.canRecvVideoHiRes() && session.isHiResVideo) {
                        inMeetingViewModel.requestHiResVideo(
                            session,
                            inMeetingViewModel.getChatId()
                        )
                    } else {
                        Timber
                        Timber.d("Already have LowRes/HiRes video, clientId ${speaker.clientId}")
                        updateListenerSpeaker(speaker.peerId, speaker.clientId, true)
                    }
                }
            }
        } else {
            Timber.d("Active video when listener is not null, clientId ${speaker.clientId}")
            speaker.videoListener?.textureView?.let { textureView ->
                textureView.parent?.let { textureViewParent ->
                    (textureViewParent as ViewGroup).removeView(textureView)
                }
            }

            surfaceContainer.addView(speaker.videoListener?.textureView)

            speaker.videoListener?.height = 0
            speaker.videoListener?.width = 0
        }

        surfaceContainer.isVisible = true
    }

    /**
     * Updating the participant who joined or left the call
     *
     * @param isAdded True, if the participant has joined. False, if the participant has left
     * @param position The position of the change
     */
    @SuppressLint("NotifyDataSetChanged")
    fun peerAddedOrRemoved(
        isAdded: Boolean,
        position: Int,
    ) {
        listView.recycledViewPool.clear()
        adapter.submitList(participants) {
            Timber.d("List updated ${adapter.currentList.size}")
            if (isAdded) {
                Timber.d("Participant added in $position")
                if (position == 0) {
                    adapter.notifyDataSetChanged()
                } else {
                    adapter.notifyItemInserted(position)
                }
            } else {
                Timber.d("Participant Removed in $position")
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, adapter.currentList.size)
            }
        }
    }

    /**
     * Update full list when the list is sorted
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateFullList() {
        listView.recycledViewPool.clear()
        adapter.submitList(participants) {
            adapter.notifyDataSetChanged()
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
            )?.let { participant ->
                Timber.d("Update the peer selected")
                inMeetingViewModel.getSession(participant.clientId)?.let { session ->
                    updateRemoteAudioVideo(TypeRemoteAVFlagChange.Audio, session)
                }
                adapter.updatePeerSelected(participant)
            }
        }
    }

    /**
     * Check changes in remote A/V flags
     *
     * @param type [TypeRemoteAVFlagChange]
     * @param session MegaChatSession of a participant
     */
    fun updateRemoteAudioVideo(type: TypeRemoteAVFlagChange, session: MegaChatSession) {
        //Speaker
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            if (session.peerid == speaker.peerId && session.clientid == speaker.clientId) {

                speaker.isAudioOn = session.hasAudio()
                speaker.isVideoOn = session.hasVideo()
                speaker.isAudioDetected = session.isAudioDetected

                when (type) {
                    TypeRemoteAVFlagChange.Video -> {
                        Timber.d("Update speaker video")
                        checkVideoOn(speaker)
                    }

                    TypeRemoteAVFlagChange.Audio -> {
                        Timber.d("Update speaker audio")
                        updateAudioIcon(speaker)
                    }

                    TypeRemoteAVFlagChange.ScreenSharing -> {
                        updateAudioIcon(speaker)
                        updateSpeakerTextViewName(speaker.name, speaker.isPresenting)
                    }
                }
            }
        }

        //Participant in list
        inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            Timber.d("Update remote A/V")
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
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            when (isCallOnHold) {
                true -> {
                    Timber.d("Speaker call is on hold")
                    videoOffUI(speaker)
                }

                false -> {
                    Timber.d("Speaker call is in progress")
                    checkVideoOn(speaker)
                }
            }
        }

        if (participants.isEmpty())
            return

        //Participant in list
        val iterator = participants.iterator()
        iterator.forEach {
            Timber.d("Update call on hold status")
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
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            if (speaker.peerId == session.peerid && speaker.clientId == session.clientid) {
                when (session.isOnHold) {
                    true -> {
                        Timber.d("Speaker session is on hold")
                        videoOffUI(speaker)
                    }

                    false -> {
                        Timber.d("Speaker session is in progress")
                        checkVideoOn(speaker)
                    }
                }
            }
        }

        //Participant in list
        inMeetingViewModel.getParticipant(
            session.peerid,
            session.clientid
        )?.let {
            Timber.d("Update session on hold status")
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
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            if (speaker.peerId == participant.peerId && speaker.clientId == participant.clientId && isHiRes) {
                Timber.d("Update listener in speaker, should the listener be added? $shouldAddListener")
                updateListenerSpeaker(speaker.peerId, speaker.clientId, shouldAddListener)
            }
        }

        //List of participants
        Timber.d("Update listener in participants list, should the listener be added? $shouldAddListener")
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
                Timber.d("Update participant name")
                adapter.updateNameOrAvatar(it, typeChange)
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
                Timber.d("Reset Size participant listener")
                listener.height = 0
                listener.width = 0
            }
        }
    }

    /**
     * Method for removing the video listener of Speaker
     */
    private fun removeSpeakerListener() {
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            Timber.d("Remove texture view of speaker")
            if (surfaceContainer.childCount > 0) {
                surfaceContainer.removeAllViews()
            }

            speaker.videoListener?.let { listener ->
                listener.textureView?.let { textureView ->
                    textureView.parent?.let { surfaceParent ->
                        (surfaceParent as ViewGroup).removeView(textureView)
                    }
                    textureView.isVisible = false
                }
            }

            Timber.d("Speaker ${speaker.clientId} video listener null")
            speaker.videoListener = null
        }
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    fun removeTextureView() {
        inMeetingViewModel.getCurrentSpeakerParticipant()?.let { speaker ->
            removeTextureViewOfPreviousSpeaker(speaker.peerId, speaker.clientId)
            inMeetingViewModel.removeSelected(
                speaker.peerId,
                speaker.clientId
            )

            speaker.videoListener?.let { listener ->
                inMeetingViewModel.removeResolutionAndListener(speaker, listener)
                removeSpeakerListener()
            }
        }

        inMeetingViewModel.removePreviousSpeakers()
        inMeetingViewModel.clearSpeakerParticipants()

        if (participants.isEmpty())
            return

        val iterator = participants.iterator()
        iterator.forEach {
            Timber.d("Remove texture view of participants")
            adapter.removeTextureView(it)
        }
    }

    override fun onDestroyView() {
        Timber.d("View destroyed")
        removeTextureView()
        super.onDestroyView()
    }
}