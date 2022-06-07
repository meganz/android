package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Pair
import android.view.*
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_CONNECTION_STATUS
import mega.privacy.android.app.constants.EventConstants.EVENT_CONTACT_NAME_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ENTER_IN_MEETING
import mega.privacy.android.app.constants.EventConstants.EVENT_ERROR_STARTING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_AVATAR_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_GET_AVATAR
import mega.privacy.android.app.constants.EventConstants.EVENT_NOT_OUTGOING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_PRIVILEGES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_REMOTE_AVFLAGS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HIRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_LOWRES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_USER_VISIBILITY_CHANGE
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.listeners.SimpleChatRequestListener
import mega.privacy.android.app.listeners.SimpleMegaRequestListener
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveX
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    AutoJoinPublicChatListener.OnJoinedChatCallback {

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    val args: InMeetingFragmentArgs by navArgs()

    // Views
    lateinit var toolbar: MaterialToolbar

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null

    private lateinit var bannerAnotherCallLayout: View
    private var bannerAnotherCallTitle: EmojiTextView? = null
    private var bannerAnotherCallSubtitle: TextView? = null

    private lateinit var bannerMuteLayout: View
    private var bannerMuteText: EmojiTextView? = null
    private var bannerMuteIcon: ImageView? = null

    private var meetingChangesBanner: EmojiTextView? = null
    private var bannerInfo: TextView? = null

    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private var swapCameraMenuItem: MenuItem? = null
    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null

    private var micIsEnable = false
    private var camIsEnable = false
    private var speakerIsEnable = false
    private var meetingLink: String = ""
    private var isManualModeView = false
    private var isWaitingForAnswerCall = false
    private var isWaitingForMakeModerator = false

    // Children fragments
    private var individualCallFragment: IndividualCallFragment? = null
    private var floatingWindowFragment: IndividualCallFragment? = null
    private var gridViewCallFragment: GridViewCallFragment? = null
    private var speakerViewCallFragment: SpeakerViewCallFragment? = null

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener
    private var bannerShouldBeShown = false

    private lateinit var binding: InMeetingFragmentBinding

    // Leave Meeting Dialog
    private var leaveDialog: Dialog? = null

    // Meeting failed Dialog
    private var failedDialog: Dialog? = null

    val inMeetingViewModel: InMeetingViewModel by activityViewModels()

    private val enableOrDisableLocalVideoObserver = Observer<Boolean> { shouldBeEnabled ->
        val chatId = inMeetingViewModel.getChatId()
        if (chatId != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null && shouldBeEnabled != camIsEnable) {
            MegaApplication.getChatManagement().isDisablingLocalVideo = true
            sharedModel.clickCamera(shouldBeEnabled)
        }
    }

    private val errorStatingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameChatRoom(it)) {
            logError("Error starting a call")
            showMeetingFailedDialog()
        }
    }

    private val nameChangeObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            logDebug("Change in name")
            updateParticipantInfo(peerId, NAME_CHANGE)
        }
    }

    // Observer for getting avatar
    private val getAvatarObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            logDebug("Change in avatar")
            updateParticipantInfo(peerId, AVATAR_CHANGE)
        }
    }

    // Observer for changing avatar
    private val avatarChangeObserver = Observer<Long> { peerId ->
        if (peerId != MegaApiJava.INVALID_HANDLE) {
            logDebug("Change in avatar")
            inMeetingViewModel.getRemoteAvatar(peerId)
        }
    }

    private val noOutgoingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameCall(it)) {
            val call = inMeetingViewModel.getCall()
            call?.let { chatCall ->
                logDebug("The call is no longer an outgoing call")
                enableOnHoldFab(chatCall.isOnHold)
            }
        }
    }

    private val visibilityChangeObserver = Observer<Long> {
        logDebug("Change in the visibility of a participant")
        inMeetingViewModel.updateParticipantsVisibility(it)
    }

    private val privilegesChangeObserver = Observer<MegaChatListItem> { item ->
        if (inMeetingViewModel.isSameChatRoom(item.chatId)) {
            inMeetingViewModel.getCall()?.let { call ->
                if (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV) && !inMeetingViewModel.isFromReconnectingStatus) {
                        logDebug("Change in my privileges")
                        if (MegaChatRoom.PRIV_MODERATOR == inMeetingViewModel.getOwnPrivileges()) {
                            showSnackbar(
                                SNACKBAR_TYPE,
                                StringResourcesUtils.getString(R.string.be_new_moderator),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }

                        inMeetingViewModel.updateOwnPrivileges()
                        bottomFloatingPanelViewHolder.updateShareAndInviteButton()
                    }

                    if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)) {
                        logDebug("Change in the privileges of a participant")
                        inMeetingViewModel.updateParticipantsPrivileges().run {
                            updateRemotePrivileges(this)
                        }
                    }
                }
            }
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
         if (inMeetingViewModel.isSameCall(it.callId)) {
            updatePanel()

            when (it.status) {
                MegaChatCall.CALL_STATUS_INITIAL -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        false,
                        inMeetingViewModel.isCallOnHold()
                    )
                }
                MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION,
                MegaChatCall.CALL_STATUS_DESTROYED -> {
                    disableCamera()
                    removeUI()
                    if (inMeetingViewModel.amIAGuest()) {
                        inMeetingViewModel.finishActivityAsGuest(meetingActivity)
                    }
                }
                MegaChatCall.CALL_STATUS_CONNECTING -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        false,
                        inMeetingViewModel.isCallOnHold()
                    )

                    if (inMeetingViewModel.isReconnectingStatus) {
                        reconnecting()
                    } else {
                        binding.reconnecting.isVisible = false
                        checkInfoBanner(TYPE_RECONNECTING)
                    }

                    checkMenuItemsVisibility()
                }
                MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        true,
                        inMeetingViewModel.isCallOnHold()
                    )

                    checkMenuItemsVisibility()
                    checkChildFragments()
                    controlVideoLocalOneToOneCall(it.hasLocalVideo())
                }
            }
        }
    }

    /**
     * Method that updates the Bottom panel
     */
    private fun updatePanel() {
        bottomFloatingPanelViewHolder.updatePanel(false)
    }

    private val callCompositionObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) &&
            it.status != INVALID_CALL_STATUS &&
            (it.callCompositionChange == 1 || it.callCompositionChange == -1)
        ) {
            if (inMeetingViewModel.isFromReconnectingStatus || inMeetingViewModel.isReconnectingStatus || !isOnline(
                    requireContext()
                )
            ) {
                logDebug("Back from reconnecting")
            } else {
                logDebug("Change in call composition, review the UI")
                if (inMeetingViewModel.isOneToOneCall()) {
                    if (it.numParticipants == 1 || it.numParticipants == 2) {
                        checkChildFragments()
                    }
                } else {
                    checkChildFragments()
                }
            }
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Change in call on hold status")
            isCallOnHold(it.isOnHold)
            checkSwapCameraMenuItemVisibility()
        }
    }

    private val localNetworkQualityObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Change in the network quality")
            checkInfoBanner(TYPE_NETWORK_QUALITY)
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showMuteBanner()
                val call = inMeetingViewModel.getCall()
                call?.let {
                    logDebug("Change in session on hold status")
                    if (!inMeetingViewModel.isOneToOneCall()) {
                        updateOnHoldRemote(callAndSession.second)
                    } else if (it.hasLocalVideo() && callAndSession.second.isOnHold) {
                        sharedModel.clickCamera(false)
                    }
                }
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<MegaChatCall, MegaChatSession>> { callAndSession ->
            if (!inMeetingViewModel.isOneToOneCall()) {
                when (callAndSession.second.status) {
                    MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                        logDebug("Session in progress, clientID = ${callAndSession.second.clientid}")
                        val position =
                            inMeetingViewModel.addParticipant(
                                callAndSession.second
                            )
                        position?.let {
                            if (position != INVALID_POSITION) {
                                checkChildFragments()
                                participantAddedOfLeftMeeting(true, it)
                            }
                        }
                    }
                    MegaChatSession.SESSION_STATUS_DESTROYED -> {
                        logDebug("Session destroyed, clientID = ${callAndSession.second.clientid}")
                        val position =
                            inMeetingViewModel.removeParticipant(callAndSession.second)
                        position.let {
                            if (position != INVALID_POSITION) {
                                checkChildFragments()
                                participantAddedOfLeftMeeting(false, it)
                            }
                        }
                    }
                }
            } else {
                checkChildFragments()
            }

            showMuteBanner()
        }

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showMuteBanner()
                if (!inMeetingViewModel.isOneToOneCall()) {
                    val isAudioChange =
                        inMeetingViewModel.changesInRemoteAudioFlag(callAndSession.second)
                    val isVideoChange =
                        inMeetingViewModel.changesInRemoteVideoFlag(callAndSession.second)
                    logDebug("Changes in AV flags. audio change $isAudioChange, video change $isVideoChange")
                    updateRemoteAVFlags(callAndSession.second, isAudioChange, isVideoChange)
                }
            }
        }

    private val chatConnectionStatusObserver =
        Observer<Pair<Long, Int>> { chatAndState ->
            if (inMeetingViewModel.isSameChatRoom(chatAndState.first) && MegaApplication.isWaitingForCall()) {
                startCall()
            }
        }

    private val sessionLowResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.getParticipant(
                    callAndSession.second.peerid,
                    callAndSession.second.clientid
                )?.let { participant ->
                    if (callAndSession.second.canRecvVideoLowRes() && callAndSession.second.isLowResVideo) {
                        if (!participant.hasHiRes) {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                logDebug("Client ID ${callAndSession.second.clientid} can receive lowRes, has lowRes, video on, checking if listener should be added...")
                                checkVideoListener(
                                    participant,
                                    shouldAddListener = true,
                                    isHiRes = false
                                )
                            }
                        }
                    } else if (!participant.hasHiRes) {
                        logDebug("Client ID ${callAndSession.second.clientid} can not receive lowRes, has lowRes, checking if listener should be removed...")
                        checkVideoListener(
                            participant,
                            shouldAddListener = false,
                            isHiRes = false
                        )

                        //I have stopped receiving LowResolution. I have to verify that I no longer need it.
                        if (callAndSession.second.hasVideo() && !inMeetingViewModel.isCallOrSessionOnHold(
                                callAndSession.second.clientid
                            )
                        ) {
                            inMeetingViewModel.getSession(callAndSession.second.clientid)
                                ?.let { session ->
                                    if (session.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS && inMeetingViewModel.isParticipantVisible(
                                            participant
                                        )
                                    ) {
                                        logDebug("Client ID ${callAndSession.second.clientid} can not receive lowRes, has lowRes, asking for low-resolution video...")
                                        inMeetingViewModel.requestLowResVideo(
                                            session,
                                            inMeetingViewModel.getChatId()
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.getParticipant(
                    callAndSession.second.peerid,
                    callAndSession.second.clientid
                )?.let { participant ->
                    if (callAndSession.second.canRecvVideoHiRes() && callAndSession.second.isHiResVideo) {
                        if (participant.hasHiRes) {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                logDebug("Client ID ${callAndSession.second.clientid} can receive hiRes, has hiRes, video on, checking if listener should be added...")
                                checkVideoListener(
                                    participant,
                                    shouldAddListener = true,
                                    isHiRes = true
                                )
                            }
                        } else {
                            if (inMeetingViewModel.sessionHasVideo(participant.clientId)) {
                                logDebug("Client ID ${callAndSession.second.clientid} can receive hiRes, has lowRes, video on, checking if listener should be added for speaker...")
                                checkSpeakerVideoListener(
                                    callAndSession.second.peerid, callAndSession.second.clientid,
                                    shouldAddListener = true
                                )
                            }
                        }
                    } else if (participant.hasHiRes) {
                        logDebug("Client ID ${callAndSession.second.clientid} can not receive hiRes, has hiRes, checking if listener should be removed...")
                        checkVideoListener(
                            participant,
                            shouldAddListener = false,
                            isHiRes = true
                        )

                        //I have stopped receiving HiResolution. I have to verify that I no longer need it.
                        if (callAndSession.second.hasVideo() && !inMeetingViewModel.isCallOrSessionOnHold(
                                callAndSession.second.clientid
                            )
                        ) {
                            inMeetingViewModel.getSession(callAndSession.second.clientid)
                                ?.let { session ->
                                    if (session.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS && inMeetingViewModel.isParticipantVisible(
                                            participant
                                        )
                                    ) {
                                        logDebug("Client ID ${callAndSession.second.clientid} can not receive hiRes, has hiRes, asking for high-resolution video...")
                                        inMeetingViewModel.requestHiResVideo(
                                            session,
                                            inMeetingViewModel.getChatId()
                                        )
                                    }
                                }
                        }
                    } else {
                        logDebug("Client ID ${callAndSession.second.clientid} can not receive hiRes, has lowRes, checking if listener of speaker should be removed...")
                        checkSpeakerVideoListener(
                            callAndSession.second.peerid, callAndSession.second.clientid,
                            shouldAddListener = false
                        )
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = InMeetingFragmentBinding.inflate(inflater)

        floatingWindowContainer = binding.selfFeedFloatingWindowContainer
        floatingBottomSheet = binding.bottomFloatingPanel.root

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logDebug("In the meeting fragment")

        initViewModel()
        MegaApplication.getInstance().startProximitySensor()
        initToolbar()
        initFloatingWindowContainerDragListener(view)
        initFloatingPanel()

        val meetingName: String = args.meetingName
        meetingName.let {
            if (!TextUtil.isTextEmpty(it)) {
                sharedModel.setMeetingsName(it)
            }
        }

        // Get meeting link from the arguments supplied when the fragment was instantiated
        meetingLink = args.meetingLink

        initLiveEventBus()
        takeActionByArgs()

        // Set on page tapping listener.
        setPageOnClickListener(view)
        checkChildFragments()
    }

    /**
     * Method for checking the different types of actions received
     */
    private fun takeActionByArgs() {
        when (args.action) {
            MEETING_ACTION_CREATE -> {
                logDebug("Action create")
                updateMicAndCam()
                initStartMeeting()
            }
            MEETING_ACTION_JOIN -> {
                logDebug("Action join")
                updateMicAndCam()

                inMeetingViewModel.joinPublicChat(
                    args.chatId,
                    AutoJoinPublicChatListener(requireContext(), this)
                )
            }
            MEETING_ACTION_REJOIN -> {
                logDebug("Action rejoin")
                updateMicAndCam()

                if (args.publicChatHandle != MEGACHAT_INVALID_HANDLE) {
                    inMeetingViewModel.rejoinPublicChat(
                        args.chatId, args.publicChatHandle,
                        AutoJoinPublicChatListener(requireContext(), this)
                    )
                }

            }
            MEETING_ACTION_GUEST -> {
                logDebug("Action guest")
                inMeetingViewModel.chatLogout(
                    SimpleChatRequestListener(
                        MegaChatRequest.TYPE_LOGOUT,
                        onSuccess = { _, _, _ ->
                            logDebug("Action guest. Log out, done")
                            inMeetingViewModel.createEphemeralAccountAndJoinChat(
                                args.firstName,
                                args.lastName,
                                SimpleMegaRequestListener(
                                    MegaRequest.TYPE_CREATE_ACCOUNT,
                                    onSuccess = { _, _, _ ->
                                        logDebug("Action guest. Create ephemeral Account, done")
                                        inMeetingViewModel.openChatPreview(
                                            meetingLink,
                                            SimpleChatRequestListener(
                                                MegaChatRequest.TYPE_LOAD_PREVIEW,
                                                onSuccess = { _, request, _ ->
                                                    logDebug(
                                                        "Action guest. Open chat preview, done. Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Call id: ${
                                                            request.megaHandleList?.get(
                                                                0
                                                            )
                                                        }"
                                                    )
                                                    MegaApplication.getChatManagement()
                                                        .setOpeningMeetingLink(
                                                            request.chatHandle,
                                                            true
                                                        )

                                                    camIsEnable =
                                                        sharedModel.cameraLiveData.value!!
                                                    speakerIsEnable =
                                                        sharedModel.speakerLiveData.value!! == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE

                                                    inMeetingViewModel.joinPublicChat(
                                                        args.chatId,
                                                        AutoJoinPublicChatListener(
                                                            context,
                                                            this@InMeetingFragment
                                                        )
                                                    )
                                                })
                                        )
                                    })
                            )
                        })
                )
            }
            MEETING_ACTION_RINGING_VIDEO_ON -> {
                logDebug("Action ringing with video on")
                inMeetingViewModel.getCall()?.let {
                    if (it.hasLocalAudio()) {
                        sharedModel.micInitiallyOn()
                    }
                    if (it.hasLocalVideo()) {
                        sharedModel.camInitiallyOn()
                    }
                }
            }
            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                logDebug("Action ringing with video off")
                inMeetingViewModel.getCall()?.let {
                    if (it.hasLocalAudio()) {
                        sharedModel.micInitiallyOn()
                    }
                }
            }
            MEETING_ACTION_START -> {
                logDebug("Action need answer call")
                updateMicAndCam()
                controlWhenJoinedAChat(inMeetingViewModel.currentChatId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pauseAudioPlayer(meetingActivity)

        // Keep screen on
        meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sendEnterCallEvent()
    }

    fun sendEnterCallEvent() = LiveEventBus.get(
        EVENT_ENTER_IN_MEETING,
        Boolean::class.java
    ).post(true)

    /**
     * Observe the Orientation changes and Update the layout for landscape and portrait screen
     *
     * @param newConfig Portrait or landscape
     */
    @Suppress("DEPRECATION")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val outMetrics = DisplayMetrics()
        val display = meetingActivity.windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        bottomFloatingPanelViewHolder.updateWidth(newConfig.orientation, outMetrics.widthPixels)

        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.updateOrientation()
            }
        }

        individualCallFragment?.let {
            if (it.isAdded) {
                it.updateOrientation()
            }
        }

        floatingWindowContainer.let {
            val menuLayoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
            menuLayoutParams.setMargins(0, 0, 0, Util.dp2px(125f, outMetrics))
            it.layoutParams = menuLayoutParams
            onConfigurationChangedOfFloatingWindow(outMetrics)
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateLayout(
                    outMetrics.widthPixels,
                    outMetrics.heightPixels
                )
            }
        }
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(EVENT_ENABLE_OR_DISABLE_LOCAL_VIDEO_CHANGE, Boolean::class.java)
            .observe(this, enableOrDisableLocalVideoObserver)

        LiveEventBus.get(EVENT_ERROR_STARTING_CALL, Long::class.java)
            .observe(this, errorStatingCallObserver)

        LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observe(this, noOutgoingCallObserver)

        LiveEventBus.get(EVENT_CONTACT_NAME_CHANGE, Long::class.java)
            .observe(this, nameChangeObserver)

        LiveEventBus.get(EVENT_PRIVILEGES_CHANGE, MegaChatListItem::class.java)
            .observe(this, privilegesChangeObserver)

        LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java)
            .observe(this, visibilityChangeObserver)

        LiveEventBus.get(EVENT_MEETING_GET_AVATAR, Long::class.java)
            .observe(this, getAvatarObserver)

        LiveEventBus.get(EVENT_MEETING_AVATAR_CHANGE, Long::class.java)
            .observe(this, avatarChangeObserver)

        //Calls level
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observe(this, callStatusObserver)

        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .observe(this, callCompositionObserver)

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observe(this, callOnHoldObserver)

        LiveEventBus.get(EVENT_LOCAL_NETWORK_QUALITY_CHANGE, MegaChatCall::class.java)
            .observe(this, localNetworkQualityObserver)

        //Sessions Level
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .observe(this, sessionStatusObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observe(this, sessionOnHoldObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observe(this, remoteAVFlagsObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HIRES_CHANGE)
            .observe(this, sessionHiResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_LOWRES_CHANGE)
            .observe(this, sessionLowResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_CHAT_CONNECTION_STATUS)
            .observe(this, chatConnectionStatusObserver as Observer<Any>)
    }

    private fun initToolbar() {
        logDebug("Update toolbar elements")
        val root = meetingActivity.binding.root
        toolbar = meetingActivity.binding.toolbar
        toolbarTitle = meetingActivity.binding.titleToolbar
        toolbarSubtitle = meetingActivity.binding.subtitleToolbar

        root.apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grey_900))
        }

        toolbar.apply {
            background = ContextCompat.getDrawable(
                requireContext(), R.drawable.gradient_shape_callschat
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        toolbarTitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        toolbarSubtitle?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        bannerAnotherCallLayout = meetingActivity.binding.bannerAnotherCall
        bannerAnotherCallTitle = meetingActivity.binding.bannerAnotherCallTitle
        bannerAnotherCallSubtitle = meetingActivity.binding.bannerAnotherCallSubtitle
        meetingChangesBanner = meetingActivity.binding.meetingBanner

        bannerInfo = meetingActivity.binding.bannerInfo
        bannerMuteLayout = meetingActivity.binding.bannerMute
        bannerMuteIcon = meetingActivity.binding.bannerMuteIcon
        bannerMuteText = meetingActivity.binding.bannerMuteText
        meetingChrono = meetingActivity.binding.simpleChronometer

        meetingActivity.setSupportActionBar(toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return

        val isGuest = arguments?.getBoolean(MEETING_IS_GUEST, false) ?: false

        if (!isGuest && args.action != MEETING_ACTION_GUEST) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        } else {
            actionBar.setHomeButtonEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
        }

        setHasOptionsMenu(true)

        bannerAnotherCallLayout.setOnClickListener {
            sharedModel.clickSwitchCall()
        }
    }

    private fun showMeetingInfoFragment() {
        MeetingInfoBottomSheetDialogFragment.newInstance()
            .run {
                show(
                    this@InMeetingFragment.childFragmentManager,
                    tag
                )
            }
    }

    /**
     * Init View Models
     */
    private fun initViewModel() {
        sharedModel.currentChatId.observe(viewLifecycleOwner) {
            it?.let {
                logDebug("Chat has changed")
                inMeetingViewModel.setChatId(it)
            }
        }

        sharedModel.meetingLinkLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                logDebug("Link has changed")
                meetingLink = it
                inMeetingViewModel.getCall()?.let {
                    if (inMeetingViewModel.isWaitingForLink()) {
                        inMeetingViewModel.setWaitingForLink(false)
                        shareLink()
                    }
                }
            }
        }

        sharedModel.meetingNameLiveData.observe(viewLifecycleOwner) {
            if (!TextUtil.isTextEmpty(it)) {
                logDebug("Meeting name has changed")
                inMeetingViewModel.updateMeetingName(it)
            }
        }

        inMeetingViewModel.callLiveData.observe(viewLifecycleOwner) {
            if (it != null && isWaitingForAnswerCall) {
                answerCallAfterJoin()
            }
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            if (toolbarTitle != null) {
                logDebug("Chat title has changed")
                toolbarTitle?.text = it
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            if (micIsEnable != it) {
                logDebug("Mic status has changed to $it")
                micIsEnable = it
                updateLocalAudio(it)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            if (camIsEnable != it) {
                logDebug("Camera status has changed to $it")
                camIsEnable = it
                updateLocalVideo(it)
            }
        }

        sharedModel.speakerLiveData.observe(viewLifecycleOwner) {
            logDebug("Speaker status has changed to $it")
            speakerIsEnable = it == AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
            updateSpeaker(it)
        }

        sharedModel.snackBarLiveData.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                showSnackbar(SNACKBAR_TYPE, it, MEGACHAT_INVALID_HANDLE)
            }
        }

        lifecycleScope.launchWhenStarted {
            sharedModel.cameraGranted.collect { allowed ->
                if (allowed) {
                    bottomFloatingPanelViewHolder.updateCamPermissionWaring(allowed)
                }
            }
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                        arrayOf(Manifest.permission.CAMERA)
                )
                        .setOnRequiresPermission { l ->
                            run {
                                onRequiresPermission(l)

                                // Continue expected action after granted
                                sharedModel.clickCamera(true)
                            }
                        }
                        .setOnShowRationale { l ->
                            onShowRationale(l)

                        }
                        .setOnNeverAskAgain { l ->
                            onPermNeverAskAgain(l) }
                        .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            sharedModel.lockMic()

            if (allowed) {
                permissionsRequester = permissionsBuilder(
                        arrayOf(Manifest.permission.RECORD_AUDIO)
                )
                        .setOnRequiresPermission { l ->
                            run {
                                onRequiresPermission(l)
                                // Continue expected action after granted
                                sharedModel.clickMic(true)
                            }
                        }
                        .setOnShowRationale { l ->
                            onShowRationale(l)
                        }
                        .setOnNeverAskAgain { l ->
                            onPermNeverAskAgain(l) }
                        .build()
                permissionsRequester.launch(false)
            }
        }

        lifecycleScope.launchWhenStarted {
            sharedModel.recordAudioGranted.collect { allowed ->
                if (allowed && !sharedModel.micLocked) {
                    bottomFloatingPanelViewHolder.updateMicPermissionWaring(allowed)
                }
            }
        }

        sharedModel.notificationNetworkState.observe(viewLifecycleOwner) { haveConnection ->
            inMeetingViewModel.updateNetworkStatus(haveConnection)
            checkInfoBanner(TYPE_NO_CONNECTION)
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateCallId.collect {
                if (it != MEGACHAT_INVALID_HANDLE) {
                    checkButtonsStatus()
                    showMuteBanner()
                    inMeetingViewModel.getCall()?.let { isCallOnHold(inMeetingViewModel.isCallOnHold()) }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.showCallDuration.collect { isVisible ->
                if (isVisible) {
                    meetingChrono?.let { chrono ->
                        chrono.stop()
                        inMeetingViewModel.getCallDuration().let {
                            if (it != INVALID_VALUE.toLong()) {
                                chrono.base = SystemClock.elapsedRealtime() - it * 1000
                                chrono.start()
                                chrono.format = " %s"
                                chrono.isVisible = true
                            }
                        }
                    }
                } else {
                    meetingChrono?.let {
                        it.stop()
                        it.isVisible = false
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateCallSubtitle.collect { type ->
                toolbarSubtitle?.let {
                    when (type) {
                        InMeetingViewModel.SubtitleCallType.TYPE_CONNECTING -> {
                            it.text = StringResourcesUtils.getString(R.string.chat_connecting)
                        }
                        InMeetingViewModel.SubtitleCallType.TYPE_CALLING -> {
                            it.text =
                                StringResourcesUtils.getString(R.string.outgoing_call_starting)
                        }
                        InMeetingViewModel.SubtitleCallType.TYPE_ESTABLISHED -> {
                            it.text = StringResourcesUtils.getString(R.string.duration_meeting)
                        }
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.allowClickingOnToolbar.collect { allowed ->
                if (allowed) {
                    meetingActivity.binding.toolbar.setOnClickListener {
                        showMeetingInfoFragment()
                    }
                } else {
                    meetingActivity.binding.toolbar.setOnClickListener(null)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.anotherChatTitle.collect { title ->
                if (title.isNotEmpty()) {
                    bannerAnotherCallTitle?.text = title
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.getParticipantsChangesText.collect { title ->
                if (title.trim().isNotEmpty()) {
                    meetingChangesBanner?.apply {
                        text = title
                        isVisible = true
                        alpha =
                                if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) 0f
                                else 1f

                        animate()?.alpha(0f)?.duration = INFO_ANIMATION.toLong()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            inMeetingViewModel.updateAnotherCallBannerType.collect { type ->
                when (type) {
                    InMeetingViewModel.AnotherCallType.TYPE_NO_CALL -> {
                        logDebug("No other calls in progress or on hold")
                        bannerAnotherCallLayout.isVisible = false
                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            false
                        )

                    }
                    InMeetingViewModel.AnotherCallType.TYPE_IN_PROGRESS -> {
                        bannerAnotherCallLayout.let {
                            it.alpha = 1f
                            it.isVisible = true
                        }
                        bannerAnotherCallSubtitle?.text =
                            StringResourcesUtils.getString(R.string.call_in_progress_layout)

                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            false
                        )

                    }
                    InMeetingViewModel.AnotherCallType.TYPE_ON_HOLD -> {
                        bannerAnotherCallLayout.let {
                            it.alpha = 0.9f
                            it.isVisible = true
                        }

                        bannerAnotherCallSubtitle?.text =
                            StringResourcesUtils.getString(R.string.call_on_hold)

                        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(
                            true
                        )
                    }
                }
            }
        }
    }

    /**
     * user denies the RECORD_AUDIO or CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onPermNeverAskAgain(permissions: java.util.ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            onAudioNeverAskAgain(permissions)
        } else  if (permissions.contains(Manifest.permission.CAMERA)) {
            onCameraNeverAskAgain(permissions)
        }
    }

    /**
     * Control the initial state of the buttons when a call is established
     */
    private fun checkButtonsStatus() {
        inMeetingViewModel.getCall()?.let {
            if (it.hasLocalAudio()) {
                sharedModel.micInitiallyOn()
            }

            if (it.hasLocalVideo()) {
                sharedModel.camInitiallyOn()
            }
            enableOnHoldFab(it.isOnHold)
            updatePanel()
            return
        }
        enableOnHoldFab(false)
    }

    /**
     * User denies the RECORD_AUDIO permission
     *
     * @param permissions permission list
     */
    private fun onAudioNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            logDebug("user denies the RECORD_AUDIO permissions")
            showRequestPermissionSnackBar()
        }
    }

    /**
     * User denies the CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onCameraNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.CAMERA)) {
            logDebug("user denies the CAMERA permission")
            showRequestPermissionSnackBar()
        }
    }

    /**
     * Create `OnDragTouchListener` and set it as `OnTouchListener` for the target view to make it draggable.
     *
     * @param parent Parent view of the draggable view.
     */
    private fun initFloatingWindowContainerDragListener(parent: View) {
        dragTouchListener = OnDragTouchListener(
            floatingWindowContainer,
            parent,
            object :
                OnDragTouchListener.OnDragActionListener {

                override fun onDragStart(view: View?) {
                    if (toolbar.isVisible) {
                        val maxTop =
                            if (bannerMuteLayout.isVisible) bannerMuteLayout.bottom else toolbar.bottom
                        dragTouchListener.setToolbarHeight(maxTop)
                        dragTouchListener.setBottomSheetHeight(floatingBottomSheet.top)
                    } else {
                        dragTouchListener.setToolbarHeight(0)
                        dragTouchListener.setBottomSheetHeight(0)
                    }
                }

                override fun onDragEnd(view: View) {
                    // Record the last Y of the floating window after dragging ended.
                    previousY = view.y
                }
            }
        )

        floatingWindowContainer.setOnTouchListener(dragTouchListener)
    }

    private fun setPageOnClickListener(view: View) = view.setOnClickListener {
        onPageClick()
    }

    fun onPageClick() {
        // If the tips is showing or bottom is fully expanded, can not hide the toolbar and panel
        if (bottomFloatingPanelViewHolder.isPopWindowShowing()
            || bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED
        ) return

        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < TAP_THRESHOLD) return

        toolbar.fadeInOut(dy = TOOLBAR_DY, toTop = true)

        if (bannerShouldBeShown) {
            bannerMuteLayout.fadeInOut(dy = FLOATING_BOTTOM_SHEET_DY, toTop = true)
        }

        floatingBottomSheet.fadeInOut(dy = FLOATING_BOTTOM_SHEET_DY, toTop = false)

        @Suppress("DEPRECATION")
        if (toolbar.isVisible) {
            meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Need not consider the snack bar
        adjustPositionOfFloatingWindow(bTop = true, bBottom = true)

        lastTouch = System.currentTimeMillis()
    }

    /**
     * Method to control the position of the floating window in relation to the configuration change
     *
     * @param outMetrics display metrics
     */
    private fun onConfigurationChangedOfFloatingWindow(outMetrics: DisplayMetrics) {
        floatingWindowContainer.post {
            previousY = -1f
            val dx = outMetrics.widthPixels - floatingWindowContainer.width
            var dy = outMetrics.heightPixels - floatingWindowContainer.height

            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder.getState() && floatingBottomSheet.isVisible) {
                dy = floatingBottomSheet.top - floatingWindowContainer.height
            }

            floatingWindowContainer.moveX(dx.toFloat())
            floatingWindowContainer.moveY(dy.toFloat())
        }
    }

    /**
     * Method to control the position of the floating window in relation to the toolbar, bottom sheet panel
     *
     * @param bTop Calculate the position related to top
     * @param bBottom Calculate the position related to bottom
     */
    private fun adjustPositionOfFloatingWindow(
        bTop: Boolean = false,
        bBottom: Boolean = true
    ) {
        var isIntersect: Boolean
        var isIntersectPreviously: Boolean

        floatingWindowContainer.apply {
            // Control the position of the floating window in relation to the toolbar, including the banner
            if (bTop) {
                val maxTop =
                    if (bannerMuteLayout.isVisible) bannerMuteLayout.bottom else toolbar.bottom

                isIntersect = (maxTop - this.y) > 0
                if (toolbar.isVisible && isIntersect) {
                    this.moveY(maxTop.toFloat())
                }

                isIntersectPreviously = (maxTop - previousY) > 0
                if (!toolbar.isVisible && isIntersectPreviously && previousY >= 0) {
                    this.moveY(previousY)
                }
            }
            if (bBottom) {
                // When the bottom panel is expanded, keep the current position
                if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    return
                }

                val bottom = this.y + this.height
                val top = floatingBottomSheet.top
                val margin1 = bottom - top

                isIntersect = margin1 > 0
                if (floatingBottomSheet.isVisible && isIntersect) {
                    this.moveY(this.y - margin1)
                }

                val margin2 = previousY + this.height - floatingBottomSheet.top
                isIntersectPreviously = margin2 > 0
                if (!floatingBottomSheet.isVisible && isIntersectPreviously && previousY >= 0) {
                    this.moveY(previousY)
                }
            }
        }
    }

    /**
     * Show the correct UI in a one-to-one call
     */
    private fun updateOneToOneUI() {
        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.amIAloneOnTheCall(it.chatid)) {
                logDebug("One to one call. Waiting for connection")
                waitingForConnection(it.chatid)
            } else {
                val session = inMeetingViewModel.getSessionOneToOneCall(it)
                session?.let {
                    logDebug("One to one call. Session exists")
                    initOneToOneCall()
                }
            }
        }
    }

    /**
     * Show the correct UI in a meeting
     */
    private fun updateGroupUI() {
        inMeetingViewModel.getCall()?.let { call ->
            if (inMeetingViewModel.amIAloneOnTheCall(call.chatid)) {
                waitingForConnection(call.chatid)
            } else {
                initGroupCall(call.chatid)
            }
        }
    }

    /**
     * Method to remove all video listeners and all child fragments
     */
    private fun removeListenersAndFragments() {
        logDebug("Remove listeners and fragments")
        removeAllListeners()
        removeAllFragments()
    }

    /**
     * Method to remove all video listeners
     */
    private fun removeAllListeners() {
        logDebug("Remove all listeners")
        individualCallFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
            }
        }

        inMeetingViewModel.removeListeners()
    }

    /**
     * Method to remove all child fragments
     */
    private fun removeAllFragments() {
        inMeetingViewModel.removeAllParticipantVisible()
        logDebug("Remove all fragments")
        floatingWindowFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                floatingWindowFragment = null
            }
        }

        individualCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                individualCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }
    }

    /**
     * Show reconnecting UI
     */
    private fun reconnecting() {
        logDebug("Show reconnecting UI, the current status is ${inMeetingViewModel.status}")
        if (inMeetingViewModel.status == NOT_TYPE)
            return

        inMeetingViewModel.status = NOT_TYPE

        removeListenersAndFragments()
        binding.reconnecting.isVisible = true
        checkInfoBanner(TYPE_RECONNECTING)
    }

    /**
     * Remove fragments
     */
    fun removeUI() {
        logDebug("Removing call UI, the current status is ${inMeetingViewModel.status}")
        if (inMeetingViewModel.status == NOT_TYPE)
            return

        inMeetingViewModel.status = NOT_TYPE

        removeListenersAndFragments()
    }

    private fun checkMenuItemsVisibility() {
        checkGridSpeakerViewMenuItemVisibility()
        checkSwapCameraMenuItemVisibility()
    }

    /**
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
        val call: MegaChatCall = inMeetingViewModel.getCall() ?: return

        logDebug("Check child fragments")
        binding.reconnecting.isVisible = false

        if (call.status >= MegaChatCall.CALL_STATUS_JOINING) {
            if (inMeetingViewModel.isOneToOneCall()) {
                logDebug("One to one call")
                updateOneToOneUI()
            } else {
                logDebug("Group call")
                updateGroupUI()
            }
        } else {
            logDebug("Waiting For Connection")
            waitingForConnection(call.chatid)
        }
    }

    /**
     * Show one to one call UI
     */
    private fun initOneToOneCall() {
        if (inMeetingViewModel.status == TYPE_IN_ONE_TO_ONE) return

        removeListenersAndFragments()

        val call: MegaChatCall? = inMeetingViewModel.getCall()
        call?.let { currentCall ->
            val session = inMeetingViewModel.getSessionOneToOneCall(currentCall)
            session?.let { userSession ->
                logDebug("Show one to one call UI")
                inMeetingViewModel.status = TYPE_IN_ONE_TO_ONE
                checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

                logDebug("Create fragment")
                individualCallFragment = IndividualCallFragment.newInstance(
                    currentCall.chatid,
                    userSession.peerid,
                    userSession.clientid
                )

                individualCallFragment?.let { it ->
                    loadChildFragment(
                        R.id.meeting_container,
                        it,
                        IndividualCallFragment.TAG
                    )
                }
            }

            initLocal(currentCall.chatid)
        }
    }

    /**
     * Method to display the waiting for connection UI
     *
     * @param chatId ID of chat
     */
    private fun waitingForConnection(chatId: Long) {
        if (inMeetingViewModel.status == TYPE_WAITING_CONNECTION) return

        logDebug("Show waiting for connection call UI")
        inMeetingViewModel.status = TYPE_WAITING_CONNECTION
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

        removeListenersAndFragments()

        individualCallFragment = IndividualCallFragment.newInstance(
            chatId,
            megaApi.myUserHandleBinary,
            false
        )

        individualCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                IndividualCallFragment.TAG
            )
        }

        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Show local fragment UI
     *
     * @param chatId the chat ID
     */
    private fun initLocal(chatId: Long) {
        logDebug("Init local fragment")
        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.removeChatVideoListener()
                removeChildFragment(it)
                floatingWindowFragment = null
            }
        }

        floatingWindowFragment = IndividualCallFragment.newInstance(
            chatId,
            megaApi.myUserHandleBinary,
            true
        )

        floatingWindowFragment?.let {
            loadChildFragment(
                R.id.self_feed_floating_window_container,
                it,
                IndividualCallFragment.TAG
            )
        }

        // Calculate the position of floating window if it is shown after the snack bar.
        meetingActivity.snackbar?.let {
            floatingWindowContainer.post {
                adjustPositionOfFloatingWindow(
                    bTop = false,
                    bBottom = true
                )
            }
        }
    }

    /**
     * Method to display the speaker view UI
     */
    private fun initSpeakerViewMode() {
        if (inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW) return

        logDebug("Show group call - Speaker View UI")
        inMeetingViewModel.status = TYPE_IN_SPEAKER_VIEW
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)
        inMeetingViewModel.removeAllParticipantVisible()

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
        speakerViewCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                SpeakerViewCallFragment.TAG
            )
        }

        inMeetingViewModel.updateParticipantResolution()
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to display the grid view UI
     */
    private fun initGridViewMode() {
        if (inMeetingViewModel.status == TYPE_IN_GRID_VIEW) return

        logDebug("Show group call - Grid View UI")
        inMeetingViewModel.status = TYPE_IN_GRID_VIEW
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

        inMeetingViewModel.removeAllParticipantVisible()

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.removeTextureView()
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }

        gridViewCallFragment = GridViewCallFragment.newInstance()
        gridViewCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                GridViewCallFragment.TAG
            )
        }

        inMeetingViewModel.updateParticipantResolution()
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to control which group UI to display
     *
     * @param chatId the chat ID
     */
    private fun initGroupCall(chatId: Long) {
        if (inMeetingViewModel.status != TYPE_IN_GRID_VIEW && inMeetingViewModel.status != TYPE_IN_SPEAKER_VIEW) {
            individualCallFragment?.let {
                if (it.isAdded) {
                    it.removeChatVideoListener()
                    removeChildFragment(it)
                    individualCallFragment = null
                }
            }
            initLocal(chatId)
        }

        when {
            !isManualModeView -> {
                inMeetingViewModel.getCall()?.let {
                    if (it.sessionsClientid.size() <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC) {
                        logDebug("Automatic mode - Grid view")
                        initGridViewMode()
                    } else {
                        logDebug("Automatic mode - Speaker view")
                        initSpeakerViewMode()
                    }
                }
            }
            inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW -> {
                logDebug("Manual mode - Speaker view")
                initSpeakerViewMode()
            }
            else -> {
                logDebug("Manual mode - Grid view")
                initGridViewMode()
            }
        }
    }

    /**
     * Method controlling whether to initiate a call
     */
    private fun initStartMeeting() {
        if (sharedModel.currentChatId.value == MEGACHAT_INVALID_HANDLE) {
            val nameChosen: String? = sharedModel.getMeetingName()
            nameChosen?.let {
                when {
                    !TextUtil.isTextEmpty(it) -> {
                        inMeetingViewModel.setTitleChat(it)
                    }
                }
            }

            logDebug("Starting meeting ...")
            startCall()
        }
    }

    /**
     * Method for loading fragments
     *
     * @param containerId The fragment ID
     * @param fragment The fragment to load
     * @param tag The specific tag to differentiate the fragment
     */
    private fun loadChildFragment(containerId: Int, fragment: Fragment, tag: String) {
        if (requireActivity().isFinishing) return

        childFragmentManager.beginTransaction().replace(
            containerId,
            fragment,
            tag
        ).commit()
    }

    private fun removeChildFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().remove(fragment).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)
        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
        swapCameraMenuItem = menu.findItem(R.id.swap_camera)

        checkMenuItemsVisibility()
    }

    /**
     * Method to show or hide the button of swap camera
     */
    private fun checkSwapCameraMenuItemVisibility() {
        swapCameraMenuItem?.let {
            it.isVisible = inMeetingViewModel.isNecessaryToShowSwapCameraOption()
        }
    }

    /**
     * Method to show or hide the buttons change to grid/speaker view
     */
    private fun checkGridSpeakerViewMenuItemVisibility() {
        inMeetingViewModel.getCall()?.let { call ->
            if (call.status == MegaChatCall.CALL_STATUS_CONNECTING) {
                gridViewMenuItem?.let {
                    it.isVisible = false
                }
                speakerViewMenuItem?.let {
                    it.isVisible = false
                }
                return
            }
        }

        when (inMeetingViewModel.status) {
            TYPE_IN_GRID_VIEW -> {
                gridViewMenuItem?.let {
                    it.isVisible = false
                }
                speakerViewMenuItem?.let {
                    it.isVisible = true
                }
            }
            TYPE_IN_SPEAKER_VIEW -> {
                gridViewMenuItem?.let {
                    it.isVisible = true
                }
                speakerViewMenuItem?.let {
                    it.isVisible = false
                }
            }
            else -> {
                gridViewMenuItem?.let {
                    it.isVisible = false
                }
                speakerViewMenuItem?.let {
                    it.isVisible = false
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.swap_camera -> {
                logDebug("Swap camera.")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(getContext()))
                true
            }
            R.id.grid_view -> {
                logDebug("Change to grid view.")
                isManualModeView = true
                initGridViewMode()
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
                isManualModeView = true
                initSpeakerViewMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        val anchor =
            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder.getState() && floatingBottomSheet.isVisible) {
                binding.snackbarPosition
            } else null

        meetingActivity.showSnackbarWithAnchorView(
            type,
            binding.root,
            anchor,
            content,
            chatId
        )
    }

    private fun showRequestPermissionSnackBar() {
        val warningText =
            StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
        showSnackbar(PERMISSIONS_TYPE, warningText, MEGACHAT_INVALID_HANDLE)
    }

    /**
     * Init Floating Panel
     */
    private fun initFloatingPanel() {
        bottomFloatingPanelViewHolder =
            BottomFloatingPanelViewHolder(
                inMeetingViewModel,
                binding,
                this,
                resources.displayMetrics
            )

        //Observer the participant List
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                bottomFloatingPanelViewHolder
                    .setParticipantsPanel(
                        it.toMutableList(),
                        inMeetingViewModel.getMyOwnInfo(
                            sharedModel.micLiveData.value ?: false,
                            sharedModel.cameraLiveData.value ?: false
                        )
                    )
            }
        }

        bottomFloatingPanelViewHolder.propertyUpdaters.add {
            toolbar.alpha = 1 - it
            // When the bottom on the top, will set the toolbar invisible, otherwise will cover the scroll event of the panel
            toolbar.isVisible = it != 1.0f
            if (bannerMuteLayout.isVisible) {
                bannerMuteLayout.alpha = 1 - it
            }

            if (bannerInfo != null && bannerInfo!!.isVisible) {
                bannerInfo!!.alpha = 1 - it
            }

            if (bannerAnotherCallLayout.isVisible) {
                bannerAnotherCallLayout.alpha = 1 - it
            }
        }
    }

    /**
     * Change Bottom Floating Panel State
     *
     */
    override fun onChangePanelState() {
        if(isWaitingForMakeModerator){
            if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                findNavController().navigate(
                    InMeetingFragmentDirections.actionGlobalMakeModerator()
                )
            }
        }
    }

    /**
     * Change Mic State
     *
     * @param micOn True, if the microphone is on. False, if the microphone is off
     */
    override fun onChangeMicState(micOn: Boolean) {
        logDebug("Change in mic state")
        sharedModel.clickMic(!micOn)
    }

    /**
     * Method for controlling the microphone status update
     *
     * @param isMicOn True, if the microphone is on. False, if the microphone is off
     */
    private fun updateLocalAudio(isMicOn: Boolean) {
        bottomFloatingPanelViewHolder.updateMicIcon(isMicOn)
        updateParticipantsBottomPanel()
        showMuteBanner()
    }

    /**
     * Method that controls whether the fixed banner should be displayed.
     * This banner is displayed when my network quality is low,
     * I am in a state of reconnection or I am alone on the call
     *
     * @param type Type of banner
     */
    private fun checkInfoBanner(type: Int) {
        bannerInfo?.let {
            val shouldShow = inMeetingViewModel.shouldShowFixedBanner(
                type
            )
            if (shouldShow) {
                showFixedBanner(it, type)
                return
            }

            if (type != TYPE_RECONNECTING) {
                val shouldShowReconnectingBanner = inMeetingViewModel.shouldShowFixedBanner(
                    TYPE_RECONNECTING,
                )
                if (shouldShowReconnectingBanner) {
                    showFixedBanner(it, TYPE_RECONNECTING)
                    return
                }
            }

            if (type != TYPE_NETWORK_QUALITY) {
                val shouldShowNetworkQualityBanner = inMeetingViewModel.shouldShowFixedBanner(
                    TYPE_NETWORK_QUALITY
                )
                if (shouldShowNetworkQualityBanner) {
                    showFixedBanner(it, TYPE_NETWORK_QUALITY)
                    return
                }
            }

            if (type != TYPE_SINGLE_PARTICIPANT) {
                val shouldShowSingleParticipantBanner =
                    inMeetingViewModel.shouldShowFixedBanner(
                        TYPE_SINGLE_PARTICIPANT
                    )
                if (shouldShowSingleParticipantBanner) {
                    showFixedBanner(it, TYPE_SINGLE_PARTICIPANT)
                    return
                }
            }

            hideFixedBanner(it)
        }
    }

    /**
     * Method of displaying the banner
     *
     * @param textView The text in the banner
     * @param type The type of banner
     */
    private fun showFixedBanner(textView: TextView, type: Int) {
        logDebug("Show fixed banner: type = $type")
        inMeetingViewModel.updateFixedBanner(textView,type)
        textView.apply {
            isVisible = true
            alpha =
                if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED) 0f
                else 1f

            if (type == TYPE_JOIN || type == TYPE_LEFT) {
                animate()?.alpha(0f)?.duration = INFO_ANIMATION.toLong()
            }
        }
    }

    private fun hideFixedBanner(textView: TextView) {
        logDebug("Hide fixed banner")
        textView.alpha = 1f
        textView.isVisible = false
    }

    /**
     * Method that controls whether the banner should be displayed.
     * This banner is displayed when the call is muted
     * or if the session or call is on hold.
     */
    private fun showMuteBanner() {
        bannerMuteLayout.let {
            bannerShouldBeShown = inMeetingViewModel.showAppropriateBanner(
                bannerMuteIcon,
                bannerMuteText
            )

            if (bannerShouldBeShown) {
                it.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.gradient_shape_callschat
                )
                logDebug("Show banner info")
                it.isVisible = true
            } else {
                logDebug("Hide banner info")
                it.isVisible = false
            }

            if (it.isVisible && (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED || !toolbar.isVisible)) {
                it.alpha = 0f
            }

            // Delay a bit to wait for 'bannerMuteLayout' finish layouting, otherwise, its bottom is 0.
            RunOnUIThreadUtils.runDelay(10) {
                adjustPositionOfFloatingWindow(
                    bTop = true,
                    bBottom = false
                )
            }
        }
    }

    /**
     * Check if a call is outgoing, on hold button must be disabled
     */
    private fun enableOnHoldFab(callIsOnHold: Boolean) {
        bottomFloatingPanelViewHolder.enableHoldIcon(
            !inMeetingViewModel.isRequestSent(),
            callIsOnHold
        )
    }

    /**
     * Method that controls whether the call has been put on or taken off hold, and updates the UI.
     */
    private fun isCallOnHold(isHold: Boolean) {
        logDebug("Changes in the on hold status of the call")
        bottomFloatingPanelViewHolder.let {
            bottomFloatingPanelViewHolder.disableEnableButtons(
                inMeetingViewModel.isCallEstablished(),
                isHold
            )
        }

        showMuteBanner()
        if (!inMeetingViewModel.isOneToOneCall()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateCallOnHold(isHold)
                }
            }

            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateCallOnHold(isHold)
                }
            }
        }
    }

    /**
     * Change Cam State
     *
     * @param camOn True, if the camera is switched on. False, if the camera is switched off
     */
    override fun onChangeCamState(camOn: Boolean) {
        logDebug("Change in camera state")
        sharedModel.clickCamera(!camOn)
    }

    /**
     * Method to disable the local camera
     */
    private fun disableCamera() {
        if (camIsEnable) {
            sharedModel.clickCamera(false)
            camIsEnable = false
        }
    }

    /**
     * Method that checks if the local video has changed in one to one call and updates the UI.
     *
     * @param isCamOn True, if the camera is switched on. False, if the camera is switched off
     */
    private fun controlVideoLocalOneToOneCall(isCamOn: Boolean) {
        val call = inMeetingViewModel.getCall()

        val shouldCheckVideoOn =
            call != null && (call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS ||
                    call.status == MegaChatCall.CALL_STATUS_JOINING) && isCamOn

        individualCallFragment?.let {
            if (it.isAdded) {
                if (shouldCheckVideoOn) {
                    it.checkVideoOn(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.videoOffUI(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                }
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                if (shouldCheckVideoOn) {
                    it.checkVideoOn(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.videoOffUI(megaApi.myUserHandleBinary, MEGACHAT_INVALID_HANDLE)
                }
            }
        }
    }

    /**
     * Method that checks if the local video has changed and updates the UI.
     *
     * @param isCamOn True, if the camera is switched on. False, if the camera is switched off
     */
    private fun updateLocalVideo(isCamOn: Boolean) {
        logDebug("Local audio or video changes")
        inMeetingViewModel.getCall()?.let {
            val isVideoOn: Boolean = it.hasLocalVideo()
            if (!MegaApplication.getChatManagement().isInTemporaryState) {
                MegaApplication.getChatManagement().setVideoStatus(it.chatid, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(isCamOn)
        updateParticipantsBottomPanel()
        checkSwapCameraMenuItemVisibility()
        controlVideoLocalOneToOneCall(isCamOn)
    }

    /*
     * Method for updating the list of participants in the bottom panel
     */
    fun updateParticipantsBottomPanel() {
        inMeetingViewModel.participants.value?.let { participants ->
            bottomFloatingPanelViewHolder
                .updateParticipants(
                    participants.toMutableList(),
                    inMeetingViewModel.getMyOwnInfo(
                        sharedModel.micLiveData.value ?: false,
                        sharedModel.cameraLiveData.value ?: false
                    )
                )
        }
    }

    /**
     * Method that checks if the session's on hold state has changed and updates the UI
     *
     * @param session The session of a participant
     */
    private fun updateOnHoldRemote(session: MegaChatSession) {
        logDebug("Changes to the on hold status of the session")
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateSessionOnHold(session)
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateSessionOnHold(session)
            }
        }
    }

    /**
     * Method that controls when the resolution of a participant's video has changed and should add or remove the listener
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     */
    private fun checkVideoListener(
        participant: Participant,
        shouldAddListener: Boolean,
        isHiRes: Boolean
    ) {
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListener(participant, shouldAddListener, isHiRes)
            }
        }
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListener(participant, shouldAddListener, isHiRes)
            }
        }
    }

    /**
     * Method that controls when the resolution of a speaker's video has changed and should add or remove the listener
     *
     * @param peerId Peer ID of participant whose listener of the video is to be added or deleted
     * @param clientId Client ID of participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     */
    private fun checkSpeakerVideoListener(
        peerId: Long, clientId: Long,
        shouldAddListener: Boolean,
    ) {
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateListenerSpeaker(peerId, clientId, shouldAddListener)
            }
        }
    }

    /**
     * Method that checks if the remote video/audio has changed and updates the UI.
     */
    private fun updateRemoteAVFlags(
        session: MegaChatSession,
        isAudioChange: Boolean,
        isVideoChange: Boolean
    ) {
        logDebug("Remote changes detected")
        gridViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                }
                if (isVideoChange) {
                    logDebug("Remote AVFlag")
                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                }
                if (isVideoChange) {
                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }
    }

    /**
     * Method that controls changes when a participant joins or leaves the call
     *
     * @param isAdded True, if added. False, if gone
     * @param position The position that has changed
     */
    private fun participantAddedOfLeftMeeting(isAdded: Boolean, position: Int) {
        logDebug("Participant was added or left the meeting in $position")
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.peerAddedOrRemoved(isAdded, position)
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.peerAddedOrRemoved(isAdded, position)
            }
        }

        inMeetingViewModel.updateParticipantResolution()
    }

    /**
     * Method that checks if a participant's name or avatar has changed and updates the UI
     *
     * @param peerId user handle that has changed
     * @param type the type of change, name or avatar
     */
    private fun updateParticipantInfo(peerId: Long, type: Int) {
        logDebug("Participant's name has changed")
        val listParticipants = inMeetingViewModel.updateParticipantsNameOrAvatar(peerId, type)
        if (listParticipants.isNotEmpty()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateNameOrAvatar(listParticipants, type)
                }
            }
            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateNameOrAvatar(listParticipants, type)
                }
            }
        }

        if (type == AVATAR_CHANGE) {
            individualCallFragment?.let {
                if (it.isAdded && inMeetingViewModel.isMe(peerId)) {
                    it.updateMyAvatar()
                }
            }

            floatingWindowFragment?.let {
                if (it.isAdded && inMeetingViewModel.isMe(peerId)) {
                    it.updateMyAvatar()
                }
            }
        }
    }

    /**
     * Method that checks if several participants privileges has changed and updates the UI
     *
     * @param listParticipants list of participants with changes
     */
    private fun updateRemotePrivileges(listParticipants: MutableSet<Participant>) {
        if (listParticipants.isNotEmpty()) {
            logDebug("Update remote privileges")
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updatePrivileges(listParticipants)
                }
            }

            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updatePrivileges(listParticipants)
                }
            }
        }
    }

    /**
     * Change Hold State
     *
     * @param isHold True, if should be on hold. False, otherwise.
     */
    override fun onChangeHoldState(isHold: Boolean) {
        val anotherCall = inMeetingViewModel.getAnotherCall()
        when {
            anotherCall == null -> {
                logDebug("No other calls in progress")
                inMeetingViewModel.setCallOnHold(isHold)
            }
            anotherCall.isOnHold -> {
                logDebug("Change of status on hold and switch of call")
                inMeetingViewModel.setCallOnHold(true)
                inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, false)
                sharedModel.clickSwitchCall()
            }
            inMeetingViewModel.isCallOnHold() -> {
                logDebug("Change of status on hold")
                inMeetingViewModel.setCallOnHold(false)
                inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, true)
            }
            else -> {
                logDebug("The current call is not on hold, change the status")
                inMeetingViewModel.setCallOnHold(isHold)
            }
        }
    }

    /**
     * Change Speaker state
     */
    override fun onChangeSpeakerState() {
        logDebug("Change in speaker state")
        sharedModel.clickSpeaker()
    }

    /**
     * Method that updates UI when speaker changes
     *
     * @param device The current device selected
     */
    private fun updateSpeaker(device: AppRTCAudioManager.AudioDevice) {
        bottomFloatingPanelViewHolder.updateSpeakerIcon(device)
    }

    /**
     * Pop up dialog for end meeting for the user/guest
     *
     * Will show bottom sheet fragment for the moderator
     */
    override fun onEndMeeting() {
        if (inMeetingViewModel.isOneToOneCall() || inMeetingViewModel.isGroupCall()) {
            logDebug("End the one to one or group call")
            leaveMeeting()
        } else if (inMeetingViewModel.shouldAssignModerator()) {
            EndMeetingBottomSheetDialogFragment.newInstance(inMeetingViewModel.getChatId())
                .run {
                    setLeaveMeetingCallBack(leaveMeetingModerator)
                    setAssignCallBack(showAssignModeratorFragment)
                    show(
                        this@InMeetingFragment.childFragmentManager,
                        tag
                    )
                }
        } else {
            askConfirmationEndMeetingForUser()
        }
    }

    private val leaveMeetingModerator = fun() {
        leaveMeeting()
    }

    /**
     * Method to navigate to the Make moderator screen
     */
    private val showAssignModeratorFragment = fun() {
        val isPanelExpanded =
            bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED
        isWaitingForMakeModerator = isPanelExpanded

        if (isPanelExpanded) {
            bottomFloatingPanelViewHolder.collapse()
        } else {
            findNavController().navigate(
                InMeetingFragmentDirections.actionGlobalMakeModerator()
            )
        }
    }

    /**
     * Dialog for confirming leave meeting action
     */
    private fun askConfirmationEndMeetingForUser() {
        leaveDialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setMessage(StringResourcesUtils.getString(R.string.title_end_meeting))
            .setPositiveButton(R.string.general_ok) { _, _ -> leaveMeeting() }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
    }

    /**
     * Method to control when I leave the call
     */
    private fun leaveMeeting() {
        logDebug("Leaving meeting")
        disableCamera()
        removeUI()

        inMeetingViewModel.leaveMeeting()
        if (inMeetingViewModel.amIAGuest()) {
            inMeetingViewModel.finishActivityAsGuest(meetingActivity)
        } else {
            sharedModel.clickEndCall()
        }
    }

    /**
     * Method to control when call ended
     */
    private fun finishActivity() {
        disableCamera()
        removeUI()
        meetingActivity.finish()
    }

    /**
     * Send share link
     *
     * @param sendLink The link of the meeting
     */
    override fun onShareLink(sendLink: Boolean) {
        if (inMeetingViewModel.isOneToOneCall() || !inMeetingViewModel.isChatRoomPublic() || inMeetingViewModel.isWaitingForLink()) {
            logError("Error getting the link, it is a private chat")
            return
        }

        if (meetingLink.isEmpty()) {
            inMeetingViewModel.setWaitingForLink(sendLink)
            sharedModel.createChatLink(
                inMeetingViewModel.getChatId(),
                inMeetingViewModel.isModerator()
            )
            logError("Error, the link doesn't exist")
            return
        }

        if (sendLink)
            shareLink()
    }

    /**
     * Method for sharing the meeting link
     */
    fun shareLink() {
        logDebug("Share the link")
        meetingActivity.startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, meetingLink)
            type = "text/plain"
        })
    }

    /**
     * Open invite participant page
     */
    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onInviteParticipants() {
        logDebug("chooseAddContactDialog")
        val inviteParticipantIntent =
            Intent(meetingActivity, AddContactActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA)
                putExtra(INTENT_EXTRA_KEY_CHAT, true)
                putExtra(INTENT_EXTRA_IS_FROM_MEETING, true)
                putExtra(INTENT_EXTRA_KEY_CHAT_ID, inMeetingViewModel.currentChatId)
                putExtra(
                    INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                    StringResourcesUtils.getString(R.string.invite_participants)
                )
            }
        meetingActivity.startActivityForResult(
            inviteParticipantIntent, REQUEST_ADD_PARTICIPANTS
        )
    }

    /**
     * Show participant bottom sheet when user click the three dots on participant item
     *
     * @param participant Participant of the meeting
     */
    override fun onParticipantOption(participant: Participant) {
        val participantBottomSheet =
            MeetingParticipantBottomSheetDialogFragment.newInstance(
                inMeetingViewModel.amIAGuest(),
                inMeetingViewModel.isModerator(),
                inMeetingViewModel.status == TYPE_IN_SPEAKER_VIEW,
                participant
            )
        participantBottomSheet.show(childFragmentManager, participantBottomSheet.tag)
    }

    companion object {

        const val INFO_ANIMATION = 4000

        const val ANIMATION_DURATION: Long = 500

        const val TAP_THRESHOLD: Long = 500

        const val TOOLBAR_DY = 300f

        const val FLOATING_BOTTOM_SHEET_DY = 400f

        const val NOT_TYPE = "NOT_TYPE"
        const val TYPE_WAITING_CONNECTION = "TYPE_WAITING_CONNECTION"
        const val TYPE_IN_ONE_TO_ONE = "TYPE_IN_ONE_TO_ONE"
        const val TYPE_IN_GRID_VIEW = "TYPE_IN_GRID_VIEW"
        const val TYPE_IN_SPEAKER_VIEW = "TYPE_IN_SPEAKER_VIEW"

        const val MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC = 6
    }

    /**
     * Method to control when a call is to be initiated
     *
     * @param chatId The chat ID of the call
     */
    private fun checkCallStarted(chatId: Long) {
        MegaApplication.getInstance().openCallService(chatId)
        inMeetingViewModel.setCall(chatId)
        checkChildFragments()
        showMuteBanner()
    }

    override fun onDestroy() {
        super.onDestroy()

        sharedModel.hideSnackBar()

        removeUI()
        logDebug("Fragment destroyed")

        resumeAudioPlayerIfNotInCall(meetingActivity)
        RunOnUIThreadUtils.stop()
        bottomFloatingPanelViewHolder.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        MegaApplication.getInstance().startProximitySensor()
        checkChildFragments()
    }

    override fun onPause() {
        super.onPause()
        MegaApplication.getInstance().unregisterProximitySensor()
    }

    override fun onStop() {
        super.onStop()
        dismissDialog(leaveDialog)
        dismissDialog(failedDialog)
    }

    /**
     * Method that updates the microphone and camera values
     */
    private fun updateMicAndCam() {
        camIsEnable = sharedModel.cameraLiveData.value!!
        micIsEnable = sharedModel.micLiveData.value!!
        bottomFloatingPanelViewHolder.updateCamIcon(camIsEnable)
        bottomFloatingPanelViewHolder.updateMicIcon(micIsEnable)
        updateParticipantsBottomPanel()
    }

    /**
     * Method to control what to do when I have joined the chatroom
     *
     * @param chatId The chat ID
     */
    private fun controlWhenJoinedAChat(chatId: Long) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            logDebug("Update chat id $chatId")
            sharedModel.updateChatRoomId(chatId)
        }

        inMeetingViewModel.checkAnotherCallsInProgress(chatId)
        if (args.action != MEETING_ACTION_GUEST || CallUtil.isStatusConnected(
                context,
                args.chatId
            )
        ) {
            answerCallAfterJoin()
        } else {
            inMeetingViewModel.registerConnectionUpdateListener(args.chatId) {
                answerCallAfterJoin()
            }
        }
    }

    override fun onJoinedChat(chatId: Long, userHandle: Long) {
        logDebug("Joined to the chat")
        controlWhenJoinedAChat(chatId)
    }

    private fun answerCallAfterJoin() {
        val call = inMeetingViewModel.getCall()
        if (call == null) {
            logDebug("Call is null")
            isWaitingForAnswerCall = true
        } else {
            isWaitingForAnswerCall = false
            logDebug("Joined to chat, answer call")
            answerCall()
        }
    }

    /**
     * Method to answer the call
     */
    private fun answerCall() {
        sharedModel.answerCall(camIsEnable, micIsEnable, speakerIsEnable).observe(viewLifecycleOwner) {
                result ->
                MegaApplication.getChatManagement().setSpeakerStatus(result.chatHandle,
                    result.enableVideo
                )
                checkCallStarted(result.chatHandle)
        }
    }

    /**
     * Method to start the call
     */
    private fun startCall() {
        inMeetingViewModel.startMeeting(camIsEnable, micIsEnable).observe(viewLifecycleOwner) {
                result ->
           result?.let {
               checkCallStarted(result.chatHandle)
           }
        }
    }

    override fun onErrorJoinedChat(chatId: Long, userHandle: Long, error: Int) {
        logDebug("Error joining the meeting so close it, error code is $error")
        finishActivity()
    }

    /**
     * The dialog for alerting the meeting is failed to created
     */
    private fun showMeetingFailedDialog() {
        failedDialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setMessage(StringResourcesUtils.getString(R.string.meeting_is_failed_content))
            .setCancelable(false)
            .setPositiveButton(R.string.general_ok) { _, _ ->
                MegaApplication.getInstance().removeRTCAudioManager()
                finishActivity()
            }.show()
    }

    /**
     * Send add contact invitation
     *
     * @param peerId the peerId of users
     */
    fun addContact(peerId: Long) {
        inMeetingViewModel.addContact(requireContext(), peerId) { content ->
            sharedModel.showSnackBar(content)
        }
    }

    /**
     * Dismiss the dialog
     *
     * @param dialog the dialog should be dismiss
     */
    fun dismissDialog(dialog: Dialog?) {
        dialog?.let {
            if (it.isShowing) it.dismiss()
        }
    }
}