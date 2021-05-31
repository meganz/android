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
import kotlinx.android.synthetic.main.speaker_view_call_fragment.*
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.databinding.SpeakerViewCallFragmentBinding
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

    private val localAudioLevelObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            speakerUser?.let {
                if (inMeetingViewModel.isSpeakerSelectionAutomatic) {
                    logDebug("Received local audio level")
                    inMeetingViewModel.assignMeAsSpeaker()
                }
            }
        }
    }

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

        //Init Speaker participant: Me
        inMeetingViewModel.assignMeAsSpeaker()
    }

    private fun initLiveEventBus() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(Constants.EVENT_REMOTE_AUDIO_LEVEL_CHANGE)
            .observeSticky(this, remoteAudioLevelObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_LOCAL_AUDIO_LEVEL_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localAudioLevelObserver)
    }

    private fun observeViewModel() {
        inMeetingViewModel.participants.observeForever(participantsObserver)

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

                if (speakerUser == null) {
                    logDebug("New speaker selected with clientId ${participantClicked.clientId}")
                    selectSpeaker(participantClicked.peerId, participantClicked.clientId)
                } else {
                    if (speakerUser!!.peerId == participantClicked.peerId && speakerUser!!.clientId == participantClicked.clientId) {
                        logDebug("Same participant, clientId ${speakerUser!!.clientId}")
                        adapter.updatePeerSelected(speakerUser!!)
                    } else {
                        logDebug("New speaker selected with clientId ${participantClicked.clientId}")
                        selectSpeaker(participantClicked.peerId, participantClicked.clientId)
                    }
                }
            })

        inMeetingViewModel.speakerParticipant.observe(
            viewLifecycleOwner
        ) {
            if (it == null) return@observe

            speakerUser?.let { currentSpeaker ->
                if (currentSpeaker.isVideoOn) {
                    logDebug("Close video of last speaker, is ${currentSpeaker.clientId}")
                    closeVideo(currentSpeaker)
                }
            }

            logDebug("Update new speaker selected with clientId ${it.clientId}")
            speakerUser = it
            speakerUser?.let { speaker ->
                updateSpeakerUser(speaker)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) { isOn ->
            speakerUser?.let {
                if (it.isMe) {
                    it.isVideoOn = isOn

                    if (isOn) {
                        checkVideoOn(it)
                    } else {
                        videoOffUI(it)
                    }
                }
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) { isOn ->
            speakerUser?.let {
                if (it.isMe) {
                    logDebug("Changes in local audio")
                    it.isAudioOn = isOn
                    updateAudioIcon(it)
                }
            }
        }
    }

    /**
     * Method that updates the number of participants visible on the recyclerview
     */
    private fun updateVisibleParticipantsSpeakerView(list: List<Participant>) =
        inMeetingViewModel.updateVisibleParticipants(list)

    /**
     * Method for selecting a new speaker
     *
     * @param peerId
     * @param clientId
     */
    private fun selectSpeaker(peerId: Long, clientId: Long) {
        logDebug("Selected new speaker with clientId $clientId")
        val listParticipants = inMeetingViewModel.updatePeerSelected(peerId, clientId)

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
        if (isSpeakerInvalid(participant)) return

        participant.avatar?.let {
            speakerAvatar.setImageBitmap(it)
        }

        updateAudioIcon(participant)

        if (participant.isVideoOn) {
            checkVideoOn(participant)
        } else {
            videoOffUI(participant)
        }
    }

    /**
     * Check if mute icon should be visible
     *
     * @param participant
     */
    private fun updateAudioIcon(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("Update audio icon, clientId ${participant.clientId}")
        speakerMuteIcon.isVisible = !participant.isAudioOn
    }

    /**
     * Show UI when video is off
     *
     * @param participant
     */
    private fun videoOffUI(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("UI video off, speaker with clientId ${participant.clientId}")
        showAvatar(participant)
        closeVideo(participant)
        checkOnHold(participant)
    }

    /**
     * Method to show the Avatar
     *
     * @param participant
     */
    private fun showAvatar(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("Show avatar")
        speakerAvatar.isVisible = true
    }

    private fun isSpeakerInvalid(toCheck: Participant): Boolean {
        if (speakerUser != null) {
            return toCheck.peerId != speakerUser!!.peerId || toCheck.clientId != speakerUser!!.clientId
        }

        return true
    }

    /**
     * Method to close Video
     *
     * @param participant
     */
    private fun closeVideo(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        surfaceContainer.isVisible = false

        if (participant.isMe) {
            logDebug("Close local video")
            closeLocalVideo(participant)
        } else {
            logDebug("Close remote video")
            inMeetingViewModel.onCloseVideo(participant)
        }

        participant.videoListener?.let { listener ->
            logDebug("Removing texture view")
            if (surfaceContainer.childCount > 0) {
                surfaceContainer.removeAllViews()
            }

            listener.textureView?.let { view ->
                view.parent?.let { viewParent ->
                    (viewParent as ViewGroup).removeView(view)
                }
            }

            logDebug("Speaker ${participant.clientId} video listener null")
            participant.videoListener = null
        }
    }

    /**
     * Method to close local video
     *
     * @param participant
     */
    private fun closeLocalVideo(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("Remove local video listener")
        inMeetingViewModel.removeLocalVideoSpeaker(
            inMeetingViewModel.getChatId(),
            participant.videoListener
        )
    }

    /**
     * Method to control the Call/Session on hold icon visibility
     *
     * @param participant
     */
    private fun checkOnHold(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        val isCallOnHold = inMeetingViewModel.isCallOnHold()

        if (participant.isMe) {
            if (isCallOnHold) {
                logDebug("Call is on hold")
                speakerOnHoldIcon.isVisible = true
                speakerAvatar.alpha = 0.5f
            } else {
                logDebug("Call is in progress")
                speakerOnHoldIcon.isVisible = false
                speakerAvatar.alpha = 1f
            }
            return
        }

        val isSessionOnHold = inMeetingViewModel.isSessionOnHold(participant.clientId)
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
     * @param participant
     */
    private fun checkVideoOn(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        if (participant.isVideoOn && ((participant.isMe && !inMeetingViewModel.isCallOnHold()) ||
                    (!participant.isMe && !inMeetingViewModel.isCallOrSessionOnHold(
                        participant.clientId
                    )))
        ) {
            logDebug("Video should be on")
            logDebug("Video should be on")
            videoOnUI(participant)
            return
        }

        logDebug("Video should be off")
        videoOffUI(participant)
    }

    /**
     * Show UI when video is on
     *
     * @param participant
     */
    private fun videoOnUI(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("UI video on, speaker with clientId ${participant.clientId}")
        hideAvatar(participant)
        activateVideo(participant)
    }

    /**
     * Method to hide the Avatar
     *
     * @param participant
     */
    private fun hideAvatar(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        logDebug("Hide Avatar")
        speakerOnHoldIcon.isVisible = false
        speakerAvatar.alpha = 1f
        speakerAvatar.isVisible = false
    }

    /**
     * Method for activating the video
     *
     * @param participant
     */
    private fun activateVideo(participant: Participant) {
        if (isSpeakerInvalid(participant)) return

        /*Video*/
        if (participant.videoListener == null) {
            logDebug("Active video when listener is null, clientId ${participant.clientId}")
            surfaceContainer.removeAllViews()

            val myTexture = TextureView(requireContext())
            myTexture.layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            myTexture.alpha = 1.0f
            myTexture.rotation = 0f

            val vListener = GroupVideoListener(
                myTexture,
                participant.peerId,
                participant.clientId,
                participant.isMe
            )

            participant.videoListener = vListener

            surfaceContainer.addView(participant.videoListener!!.textureView)

            if (participant.isMe) {
                inMeetingViewModel.addLocalVideoSpeaker(
                    inMeetingViewModel.getChatId(),
                    vListener
                )
            } else {
                inMeetingViewModel.onActivateVideo(participant, true)
            }
        } else {
            logDebug("Active video when listener is not null, clientId ${participant.clientId}")
            if (surfaceContainer.childCount > 0) {
                surfaceContainer.removeAllViews()
            }

            participant.videoListener?.textureView?.let { textureView ->
                textureView.parent?.let { textureViewParent ->
                    (textureViewParent as ViewGroup).removeView(textureView)
                }
            }

            surfaceContainer.addView(participant.videoListener?.textureView)

            participant.videoListener?.height = 0
            participant.videoListener?.width = 0
        }

        surfaceContainer.isVisible = true
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
                inMeetingViewModel.onActivateVideo(
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
        inMeetingViewModel.getParticipant(
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
            inMeetingViewModel.getParticipant(
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

        const val TAG = "SpeakerViewCallFragment"

        @JvmStatic
        fun newInstance() = SpeakerViewCallFragment()
    }

    override fun onResume() {
        val iterator = participants.iterator()
        iterator.forEach {
            inMeetingViewModel.resetSizeListener(it)
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
        inMeetingViewModel.getParticipant(
            peerId,
            clientId
        )?.let {
            inMeetingViewModel.resetSizeListener(it)
        }
    }

    /**
     * Method to delete the videos and texture views of participants
     */
    private fun removeTextureView() {
        speakerUser?.let {
            inMeetingViewModel.removeSelected(
                it.peerId,
                it.clientId
            )

            if (it.isMe) {
                logDebug("Close local video")
                closeLocalVideo(it)
            } else {
                logDebug("Close remote video")
                inMeetingViewModel.onCloseVideo(
                    it
                )
            }

            logDebug("Remove texture view")
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

                logDebug("Speaker ${it.clientId} video listener null")
                it.videoListener = null
            }
        }

        val iterator = participants.iterator()
        iterator.forEach {
            inMeetingViewModel.onCloseVideo(it)
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
        inMeetingViewModel.participants.removeObserver(
            participantsObserver
        )
        super.onDestroy()
    }
}