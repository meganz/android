package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_assign_moderator.view.*
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_CONNECTION_STATUS
import mega.privacy.android.app.constants.EventConstants.EVENT_CONTACT_NAME_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_ENTER_IN_MEETING
import mega.privacy.android.app.constants.EventConstants.EVENT_ERROR_STARTING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_LOCAL_NETWORK_QUALITY_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_INVITE
import mega.privacy.android.app.constants.EventConstants.EVENT_NOT_OUTGOING_CALL
import mega.privacy.android.app.constants.EventConstants.EVENT_PRIVILEGES_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_PROXIMITY_SENSOR_CHANGE
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
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    StartChatCallListener.OnCallStartedCallback, AnswerChatCallListener.OnCallAnsweredCallback,
    AutoJoinPublicChatListener.OnJoinedChatCallback {

    private var bShowImcompatibility: Boolean = false
    private var bInviteSent: Boolean = false
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

    private var bannerParticipant: EmojiTextView? = null

    private var bannerInfo: TextView? = null

    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private var swapCameraMenuItem: MenuItem? = null
    private var gridViewMenuItem: MenuItem? = null
    private var speakerViewMenuItem: MenuItem? = null

    private var micIsEnable = false
    private var camIsEnable = false
    private var meetinglink: String = ""
    private var inTemporaryState = false
    private var isManualModeView = false
    private var isWaitingForAnswerCall = false

    // Children fragments
    private var individualCallFragment: IndividualCallFragment? = null
    private var floatingWindowFragment: IndividualCallFragment? = null
    private var gridViewCallFragment: GridViewCallFragment? = null
    private var speakerViewCallFragment: SpeakerViewCallFragment? = null

    private var status = NOT_TYPE

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener
    private var bannerShouldBeShown = false

    private lateinit var binding: InMeetingFragmentBinding

    val inMeetingViewModel by viewModels<InMeetingViewModel>()

    // Create Timer
    var channel = Channel<Int>(capacity = 1)

    private val proximitySensorChangeObserver = Observer<Boolean> {
        val chatId = inMeetingViewModel.getChatId()
        if (chatId != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null) {
            val realStatus = MegaApplication.getChatManagement().getVideoStatus(chatId)
            when {
                !realStatus -> {
                    inTemporaryState = false
                }
                it -> {
                    logError("Proximity sensor, video off")
                    inTemporaryState = true
                    sharedModel.clickCamera(false)
                }
                else -> {
                    logError("Proximity sensor, video on")
                    inTemporaryState = false
                    sharedModel.clickCamera(true)
                }
            }
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
            updateParticipantName(peerId)
        }
    }

    private val noOutgoingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameCall(it)) {
            val call = inMeetingViewModel.getCall()
            call?.let { chatCall ->
                logDebug("The call is no longer an outgoing call")
                updateToolbarSubtitle(chatCall)
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
                        bottomFloatingPanelViewHolder.updatePrivilege(inMeetingViewModel.getOwnPrivileges())
                    }

                    if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)) {
                        logDebug("Change in the privileges of a participant")
                        inMeetingViewModel.updateParticipantsPrivileges().run {
                            updateRemotePrivileges(this)
                            bottomFloatingPanelViewHolder.updateRemotePrivileges(this)
                        }
                    }
                }
            }
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
        if (it.status == INVALID_CALL_STATUS) {
            checkAnotherCall()
        } else if (inMeetingViewModel.isSameCall(it.callId)) {
            updateToolbarSubtitle(it)
            updatePanelAndToolbar(it)

            when (it.status) {
                MegaChatCall.CALL_STATUS_INITIAL, MegaChatCall.CALL_STATUS_JOINING -> {
                    bottomFloatingPanelViewHolder.disableEnableButtons(
                        false,
                        inMeetingViewModel.isCallOnHold()
                    )
                }
                MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED -> finishActivity()
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
                    checkCurrentParticipants()
                    checkMenuItemsVisibility()
                    checkChildFragments()

                    if (it.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                        controlVideoLocalOneToOneCall(it.hasLocalVideo())
                    }
                }
            }
        }
    }

    /**
     * Method that updates the Bottom panel and toolbar depending the call status
     *
     * @param call The current call
     */
    private fun updatePanelAndToolbar(call: MegaChatCall?) {
        toolbar.setOnClickListener {
            if (call?.status == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                showMeetingInfoFragment()
            }
        }
        bottomFloatingPanelViewHolder.updateMeetingType(!inMeetingViewModel.isOneToOneCall())
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
                    if (it.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && !inMeetingViewModel.isRequestSent()) {
                        showParticipantBanner(
                            it.peeridCallCompositionChange,
                            it.callCompositionChange
                        )
                    }

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
        } else {
            checkAnotherCall()
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
            } else {
                checkAnotherCall()
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (!inMeetingViewModel.isSameCall(callAndSession.first)) {
                checkAnotherCall()
                return@Observer
            }

            if (!inMeetingViewModel.isOneToOneCall()) {
                when (callAndSession.second.status) {
                    MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                        logDebug("Session in progress")
                        val position =
                            inMeetingViewModel.addParticipant(
                                callAndSession.second,
                                status
                            )
                        position?.let {
                            if (position != INVALID_POSITION) {
                                checkChildFragments()
                                participantAddedOfLeftMeeting(true, it)
                            }
                        }
                    }
                    MegaChatSession.SESSION_STATUS_DESTROYED -> {
                        logDebug("Session destroyed")
                        val position =
                            inMeetingViewModel.removeParticipant(callAndSession.second)
                        position?.let {
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
                inMeetingViewModel.startMeeting(
                    camIsEnable,
                    micIsEnable,
                    StartChatCallListener(meetingActivity, this, this)
                )
            }
        }

    private val sessionLowResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                if (callAndSession.second.canRecvVideoLowRes()) {
                    logDebug("Can receive low-resolution video")
                } else {
                    logDebug("Can not receive low-resolution video")
                    //Check list of participants
                    if (inMeetingViewModel.changesInLowRes(callAndSession.second)) {
                        updateLowOrHiResolution(false, callAndSession.second)
                    }
                }
            }
        }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                if (callAndSession.second.canRecvVideoHiRes()) {
                    logDebug("Can receive high-resolution video")
                } else {
                    logDebug("Can not receive high-resolution video")
                    //Check individual call
                    individualCallFragment?.let {
                        if (it.isAdded) {
                            it.updateResolution(
                                callAndSession.second.peerid,
                                callAndSession.second.clientid
                            )
                        }
                    }

                    //Check list of participants
                    if (inMeetingViewModel.changesInHiRes(callAndSession.second)) {
                        updateLowOrHiResolution(true, callAndSession.second)
                    }
                }
            }
        }

    private val inviteObserver =
        Observer<Boolean> { invite ->
            if (invite && !bInviteSent){
                bInviteSent = true
                launchTimer()
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
        MegaApplication.getInstance().startProximitySensor()
        initToolbar()
        initFloatingWindowContainerDragListener(view)
        initFloatingPanel()

        var chatId: Long? =
            arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)

        if (chatId == MEGACHAT_INVALID_HANDLE) {
            sharedModel.currentChatId.value?.let {
                chatId = it
            }
        }

        chatId?.let {
            if (it != MEGACHAT_INVALID_HANDLE) {
                if (it == inMeetingViewModel.getChatId()) {
                    logDebug("Same chat")
                } else {
                    logDebug("Different chat")
                    sharedModel.updateChatRoomId(it)
                    inMeetingViewModel.setChatId(it)
                }
            }
        }

        val meetingName: String = args.meetingName
        meetingName.let {
            if (!TextUtil.isTextEmpty(it)) {
                sharedModel.setMeetingsName(it)
            }
        }

        // Get meeting link from the arguments supplied when the fragment was instantiated
        meetinglink = args.meetingLink

        val isAudioEnable: Boolean? =
            arguments?.getBoolean(MeetingActivity.MEETING_AUDIO_ENABLE, false)

        isAudioEnable?.let { if (it) sharedModel.micInitiallyOn() }

        val isVideoEnable: Boolean? =
            arguments?.getBoolean(MeetingActivity.MEETING_VIDEO_ENABLE, false)

        isVideoEnable?.let { if (it) sharedModel.camInitiallyOn() }

        val currentCall: MegaChatCall? = inMeetingViewModel.getCall()
        if (currentCall != null && currentCall.status > MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
            if (currentCall.hasLocalAudio()) {
                sharedModel.micInitiallyOn()
            }

            if (currentCall.hasLocalVideo()) {
                sharedModel.camInitiallyOn()
            }

            updateToolbarSubtitle(currentCall)
            enableOnHoldFab(currentCall.isOnHold)
            updatePanelAndToolbar(currentCall)
        } else {
            enableOnHoldFab(false)
        }

        initLiveEventBus()
        initViewModel()
        takeActionByArgs()

        // Set on page tapping listener.
        setPageOnClickListener(view)
        checkChildFragments()
        checkCurrentParticipants()
        checkAnotherCall()
        inMeetingViewModel.getCall()?.let { isCallOnHold(inMeetingViewModel.isCallOnHold()) }
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
                inMeetingViewModel.chatLogout(SimpleChatRequestListener(MegaChatRequest.TYPE_LOGOUT, onSuccess = { _, _, _ ->
                    inMeetingViewModel.createEphemeralAccountAndJoinChat(
                        args.firstName,
                        args.lastName,
                        SimpleMegaRequestListener(MegaRequest.TYPE_CREATE_ACCOUNT, onSuccess = { _, _, _ ->
                            inMeetingViewModel.chatConnect(SimpleChatRequestListener(MegaChatRequest.TYPE_CONNECT, onSuccess = { _, _, _ ->
                                inMeetingViewModel.openChatPreview(
                                    meetinglink,
                                    SimpleChatRequestListener(MegaChatRequest.TYPE_LOAD_PREVIEW, onSuccess = { _, request, _ ->
                                        logDebug(
                                            "Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Call id: ${
                                                request.megaHandleList?.get(
                                                    0
                                                )
                                            }"
                                        )
                                        MegaApplication.getChatManagement().setOpeningMeetingLink(
                                            request.chatHandle,
                                            true
                                        )
                                        camIsEnable = sharedModel.cameraLiveData.value!!
                                        inMeetingViewModel.joinPublicChat(
                                            args.chatId,
                                            AutoJoinPublicChatListener(
                                                context,
                                                this@InMeetingFragment
                                            )
                                        )
                                    })
                                )
                            }))
                        })
                    )
                }))
            }
            MEETING_ACTION_RINGING_VIDEO_ON -> {
                logDebug("Action ringing with video on")
                sharedModel.micInitiallyOn()
                sharedModel.camInitiallyOn()
            }
            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                logDebug("Action ringing with video off")
                sharedModel.micInitiallyOn()
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
        // Set parent activity can receive the orientation changes
        meetingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

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
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val display = meetingActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        bottomFloatingPanelViewHolder.updateWidth(newConfig.orientation, outMetrics.widthPixels)

        floatingWindowContainer.let {
            val menuLayoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
            menuLayoutParams.setMargins(0, 0, 0, Util.dp2px(125f, outMetrics))
            it.layoutParams = menuLayoutParams
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                it.updateOrientation()
            }
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
        LiveEventBus.get(EVENT_PROXIMITY_SENSOR_CHANGE, Boolean::class.java)
            .observe(this, proximitySensorChangeObserver)

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
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_MEETING_INVITE)
            .observe(this, inviteObserver as Observer<Any>)
    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbarTitle = meetingActivity.title_toolbar
        toolbarSubtitle = meetingActivity.subtitle_toolbar
        toolbarSubtitle?.let {
            it.text = StringResourcesUtils.getString(R.string.chat_connecting)
        }

        bannerAnotherCallLayout = meetingActivity.banner_another_call
        bannerAnotherCallTitle = meetingActivity.banner_another_call_title
        bannerAnotherCallSubtitle = meetingActivity.banner_another_call_subtitle
        bannerParticipant = meetingActivity.banner_participant
        bannerInfo = meetingActivity.banner_info
        bannerMuteLayout = meetingActivity.banner_mute
        bannerMuteIcon = meetingActivity.banner_mute_icon
        bannerMuteText = meetingActivity.banner_mute_text
        meetingChrono = meetingActivity.simple_chronometer

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
        }

        setHasOptionsMenu(true)

        bannerAnotherCallLayout.setOnClickListener {
            returnToAnotherCall()
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
                meetinglink = it
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
                inMeetingViewModel.setTitleChat(it)
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
            updateSpeaker(it)
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresCameraPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickCamera(true)
                            bottomFloatingPanelViewHolder.updateCamPermissionWaring(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                    .build().launch(false)
            }
        }
        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresAudioPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickMic(true)
                            bottomFloatingPanelViewHolder.updateMicPermissionWaring(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                    .build().launch(false)
            }
        }

        sharedModel.snackBarLiveData.observe(viewLifecycleOwner) {
            showSnackbar(SNACKBAR_TYPE, it, MEGACHAT_INVALID_HANDLE)
            if (bInviteSent) {
                bInviteSent = false;
                val count = inMeetingViewModel.participants.value
                if (count != null) {
                    var participantsCount = getparticipantsCount()
                    if(participantsCount == 0L && count.size == 0){
                        launchTimer()
                    } else if (participantsCount > 0 && count.size < participantsCount.toInt()) {
                        // start timer
                        launchTimer()
                    } else {
                        logDebug("noting to do")
                    }
                }

            }
        }

        sharedModel.cameraGranted.observe(viewLifecycleOwner) {
            bottomFloatingPanelViewHolder.updateCamPermissionWaring(it)
        }
        sharedModel.recordAudioGranted.observe(viewLifecycleOwner) {
            bottomFloatingPanelViewHolder.updateMicPermissionWaring(it)
        }

        sharedModel.notificationNetworkState.observe(viewLifecycleOwner) { haveConnection ->
            inMeetingViewModel.updateNetworkStatus(haveConnection)
            checkInfoBanner(TYPE_NO_CONNECTION)
        }
    }

    private fun getparticipantsCount(): Long {
        val chat = megaChatApi.getChatRoom(inMeetingViewModel.currentChatId)
        return if (chat == null) {
            -1L
        } else {
            logDebug("all participants: $chat.peerCount")
            chat.peerCount
        }
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

        if (toolbar.isVisible) {
            meetingActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            meetingActivity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        checkRelativePositionWithToolbar()
        checkRelativePositionWithBottomSheet()

        lastTouch = System.currentTimeMillis()
    }

    /**
     * Method to control the position of the floating window in relation to the toolbar
     */
    private fun checkRelativePositionWithToolbar() {
        val maxTop = if (bannerMuteLayout.isVisible) bannerMuteLayout.bottom else toolbar.bottom

        val isIntersect = (maxTop - floatingWindowContainer.y) > 0
        if (toolbar.isVisible && isIntersect) {
            floatingWindowContainer.moveY(maxTop.toFloat())
        }

        val isIntersectPreviously = (maxTop - previousY) > 0
        if (!toolbar.isVisible && isIntersectPreviously && previousY >= 0) {
            floatingWindowContainer.moveY(previousY)
        }
    }

    /**
     * Method to control the position of the floating window in relation to the bottom sheet panel
     */
    private fun checkRelativePositionWithBottomSheet() {
        val bottom = floatingWindowContainer.y + floatingWindowContainer.height
        val top = floatingBottomSheet.top
        val margin1 = bottom - top

        val isIntersect = margin1 > 0
        if (floatingBottomSheet.isVisible && isIntersect) {
            floatingWindowContainer.moveY(floatingWindowContainer.y - margin1)
        }

        val margin2 = previousY + floatingWindowContainer.height - floatingBottomSheet.top
        val isIntersectPreviously = margin2 > 0
        if (!floatingBottomSheet.isVisible && isIntersectPreviously && previousY >= 0) {
            floatingWindowContainer.moveY(previousY)
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
     * Method to return to another call if it exists
     */
    private fun returnToAnotherCall() {
        val anotherCall = inMeetingViewModel.getAnotherCall()
        if (anotherCall != null) {
            logDebug("Return to another call")
            CallUtil.openMeetingInProgress(requireContext(), anotherCall.chatid, false)
        }
    }

    /**
     * Check whether or not there are calls on hold in other chats.
     */
    private fun checkAnotherCall() {
        val anotherCall = inMeetingViewModel.getAnotherCall()
        if (anotherCall != null) {
            logDebug("Another call exists")
            updateOnHoldFabButton(anotherCall)
            updateBannerAnotherCall(anotherCall)
            return
        }

        bottomFloatingPanelViewHolder.changeOnHoldIconDrawable(false)

        logDebug("No other calls in progress or on hold")
        bannerAnotherCallLayout.isVisible = false
    }

    /**
     * Update the button if there are calls on hold in other chats or not.
     *
     * @param anotherCall Another call on hold or in progress
     */
    private fun updateOnHoldFabButton(anotherCall: MegaChatCall) {
        bottomFloatingPanelViewHolder.changeOnHoldIcon(
            anotherCall.isOnHold
        )
    }

    /**
     * Update the banner if there are calls on hold in other chats or not.
     *
     * @param anotherCall Another call on hold or in progress
     */
    private fun updateBannerAnotherCall(anotherCall: MegaChatCall) {
        logDebug("Show banner of another call")
        var isOnHold = anotherCall.isOnHold
        if (inMeetingViewModel.isAnotherCallOneToOneCall(anotherCall.chatid) && inMeetingViewModel.isSessionOnHoldAnotherOneToOneCall(
                anotherCall
            )
        ) {
            isOnHold = true
        }

        val anotherChat = megaChatApi.getChatRoom(anotherCall.chatid)
        bannerAnotherCallTitle?.let {
            it.text = getTitleChat(anotherChat)
        }

        bannerAnotherCallSubtitle?.let {
            it.text = StringResourcesUtils.getString(
                if (isOnHold) R.string.call_on_hold
                else R.string.call_in_progress_layout
            )

            it.alpha = 1f
        }

        bannerAnotherCallLayout.let {
            it.alpha = if (isOnHold) 0.9f else 1f
            it.isVisible = true

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
     * Method to check if there are already participants in the call
     */
    private fun checkCurrentParticipants() {
        inMeetingViewModel.getCall()?.let {
            logDebug("Check current call participants")
            inMeetingViewModel.createCurrentParticipants(it.sessionsClientid, status)
        }
    }

    /**
     * Show reconnecting UI
     */
    private fun reconnecting() {
        logDebug("Show reconnecting UI")
        removeUI()
        binding.reconnecting.isVisible = true
        checkInfoBanner(TYPE_RECONNECTING)
    }

    /**
     * Remove fragments
     */
    private fun removeUI() {
        if (status == NOT_TYPE)
            return

        status = NOT_TYPE
        MegaApplication.getInstance().unregisterProximitySensor()

        individualCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                individualCallFragment = null
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                floatingWindowFragment = null
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                gridViewCallFragment = null
            }
        }
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
        if (status == TYPE_IN_ONE_TO_ONE) return

        val call: MegaChatCall? = inMeetingViewModel.getCall()
        call?.let { currentCall ->
            val session = inMeetingViewModel.getSessionOneToOneCall(currentCall)
            session?.let { userSession ->
                logDebug("Show one to one call UI")
                status = TYPE_IN_ONE_TO_ONE
                checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

                individualCallFragment?.let {
                    if (it.isAdded) {
                        removeChildFragment(it)
                        individualCallFragment = null
                    }
                }

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
        if (status == TYPE_WAITING_CONNECTION) return

        logDebug("Show waiting for connection call UI")
        status = TYPE_WAITING_CONNECTION
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

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
        if (floatingWindowFragment == null) {
            floatingWindowFragment = IndividualCallFragment.newInstance(
                chatId,
                megaApi.myUserHandleBinary,
                true
            )
        }

        floatingWindowFragment?.let {
            loadChildFragment(
                R.id.self_feed_floating_window_container,
                it,
                IndividualCallFragment.TAG
            )
        }
    }

    /**
     * Method to display the speaker view UI
     */
    private fun initSpeakerViewMode() {
        if (status == TYPE_IN_SPEAKER_VIEW) return

        logDebug("Show group call - Speaker View UI")
        status = TYPE_IN_SPEAKER_VIEW
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

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

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
        speakerViewCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                SpeakerViewCallFragment.TAG
            )
        }

        inMeetingViewModel.removeParticipantResolution(status)
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to display the grid view UI
     */
    private fun initGridViewMode() {
        if (status == TYPE_IN_GRID_VIEW) return

        logDebug("Show group call - Grid View UI")
        status = TYPE_IN_GRID_VIEW
        checkInfoBanner(TYPE_SINGLE_PARTICIPANT)

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                removeChildFragment(it)
                speakerViewCallFragment = null
            }
        }

        gridViewCallFragment?.let {
            if (it.isAdded) {
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

        inMeetingViewModel.removeParticipantResolution(status)
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Method to control which group UI to display
     *
     * @param chatId the chat ID
     */
    private fun initGroupCall(chatId: Long) {
        if (status != TYPE_IN_GRID_VIEW && status != TYPE_IN_SPEAKER_VIEW) {
            initLocal(chatId)
        }

        when {
            !isManualModeView -> {
                inMeetingViewModel.getCall()?.let {
                    if (it.numParticipants <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC) {
                        logDebug("Automatic mode - Grid view")
                        initGridViewMode()
                    } else {
                        logDebug("Automatic mode - Speaker view")
                        initSpeakerViewMode()
                    }
                }
            }
            status == TYPE_IN_SPEAKER_VIEW -> {
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
            inMeetingViewModel.startMeeting(
                camIsEnable,
                micIsEnable,
                StartChatCallListener(requireContext(), this, this)
            )
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

    /**
     * Method that updates the toolbar caption depending on the status of the call
     *
     * @param call MegaChatCall
     */
    private fun updateToolbarSubtitle(call: MegaChatCall) {
        logDebug("Call status is " + CallUtil.callStatusToString(call.status))
        when (call.status) {
            MegaChatCall.CALL_STATUS_CONNECTING -> {
                CallUtil.activateChrono(false, meetingChrono, null)
                toolbarSubtitle?.let {
                    it.text = StringResourcesUtils.getString(R.string.chat_connecting)
                }
            }

            MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                val chatRoom = inMeetingViewModel.getChat()
                val isMeeting = chatRoom?.isMeeting
                val isGroup = chatRoom?.isGroup
                if (inMeetingViewModel.isRequestSent() && chatRoom != null && !chatRoom.isMeeting) {
                    CallUtil.activateChrono(false, meetingChrono, null)
                    toolbarSubtitle?.let {
                        it.text = StringResourcesUtils.getString(R.string.outgoing_call_starting)
                    }
                    launchTimer()
                } else {
                    val chatRoom = inMeetingViewModel.getChat()
                    if(chatRoom != null && !chatRoom.isMeeting){
                        channel.cancel()
                        meetingActivity.snackbar?.dismiss()
                    }
                    toolbarSubtitle?.let {
                        it.text = StringResourcesUtils.getString(R.string.duration_meeting)
                    }

                    CallUtil.activateChrono(true, meetingChrono, call)
                }
            }
        }
    }

    /**
     * Start Timer for 26s and show snackbar for version incompatibility
     */
    private fun launchTimer() {
        if(!bShowImcompatibility) {
            bShowImcompatibility = true
            lifecycleScope.launch {
                launch(Dispatchers.IO) {
                    logDebug("launchTimer() will send after 26s")
                    delay(WAITING_TIME)
                    if(!channel.isClosedForSend) {
                        logDebug("launchTimer() send and then close")
                        channel.send(WAITING_TIME.toInt())
                        channel.close()
                    }
                }
                launch(Dispatchers.IO) {
                    for (element in channel) {
                        logDebug("launchTimer() receive $element")
                        // After 26 seconds if the call is still not answered, they will see a message
                        activity?.runOnUiThread {
                            showSnackbar(
                                SNACKBAR_IMCOMPATIBILITY_TYPE,
                                StringResourcesUtils.getString(
                                    R.string.version_incompatibility
                                ),
                                MEGACHAT_INVALID_HANDLE
                            )
                        }
                    }
                }
            }
        }
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

        when (status) {
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
            if (BottomSheetBehavior.STATE_COLLAPSED == bottomFloatingPanelViewHolder.getState()) {
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
                !inMeetingViewModel.isOneToOneCall()
            )

        updatePanelParticipantList()

        //Observer the participant List
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                updatePanelParticipantList(it.toMutableList())

                // Check current the count of participants
                var participantsCount = getparticipantsCount()
                val count = it.size
                if (participantsCount > 0 && count == participantsCount.toInt()) {
                    // all participants in and stop timer and remove snackbar
                    channel.cancel()
                    meetingActivity.snackbar?.dismiss()
                }
                if(participantsCount > 0 && count < participantsCount.toInt()){
                    launchTimer()
                }
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
        showMuteBanner()
    }

    /**
     * Method that controls whether the fixed banner should be displayed.
     * This banner is displayed when a participant joins/leaves the meeting
     *
     * @param peerId userHandle
     * @param type Type of banner
     */
    private fun showParticipantBanner(peerId: Long, type: Int) {
        bannerParticipant?.let {
            showFixedBanner(it, peerId, type)
        }
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
                showFixedBanner(it, MEGACHAT_INVALID_HANDLE, type)
                return
            }

            if (type != TYPE_RECONNECTING) {
                val shouldShowReconnectingBanner = inMeetingViewModel.shouldShowFixedBanner(
                    TYPE_RECONNECTING,
                )
                if (shouldShowReconnectingBanner) {
                    showFixedBanner(it, MEGACHAT_INVALID_HANDLE, TYPE_RECONNECTING)
                    return
                }
            }

            if (type != TYPE_NETWORK_QUALITY) {
                val shouldShowNetworkQualityBanner = inMeetingViewModel.shouldShowFixedBanner(
                    TYPE_NETWORK_QUALITY
                )
                if (shouldShowNetworkQualityBanner) {
                    showFixedBanner(it, MEGACHAT_INVALID_HANDLE, TYPE_NETWORK_QUALITY)
                    return
                }
            }

            if (type != TYPE_SINGLE_PARTICIPANT) {
                val shouldShowSingleParticipantBanner =
                    inMeetingViewModel.shouldShowFixedBanner(
                        TYPE_SINGLE_PARTICIPANT
                    )
                if (shouldShowSingleParticipantBanner) {
                    showFixedBanner(it, MEGACHAT_INVALID_HANDLE, TYPE_SINGLE_PARTICIPANT)
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
     * @param peerId User handle
     * @param type The type of banner
     */
    private fun showFixedBanner(textView: TextView, peerId: Long, type: Int) {
        logDebug("Show fixed banner: type = $type")
        inMeetingViewModel.updateFixedBanner(textView, peerId, type)
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
                checkRelativePositionWithToolbar()
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
     * Method that checks if the local video has changed in one to one call and updates the UI.
     *
     * @param isCamOn True, if the camera is switched on. False, if the camera is switched off
     */
    private fun controlVideoLocalOneToOneCall(isCamOn: Boolean) {
        val call = inMeetingViewModel.getCall()
        val shouldCheckVideoOn =
            call != null && call.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && isCamOn

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
            if (!inTemporaryState) {
                MegaApplication.getChatManagement().setVideoStatus(it.chatid, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(isCamOn)
        checkSwapCameraMenuItemVisibility()
        controlVideoLocalOneToOneCall(isCamOn)
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
     * Method that controls when the resolution of a participant's video has changed
     *
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     * @param session The session of a participant
     */
    private fun updateLowOrHiResolution(isHiRes: Boolean, session: MegaChatSession) {
        logDebug("The resolution of a participant needs to be updated")
        inMeetingViewModel.getParticipant(session.peerid, session.clientid)?.let {
            val speakerParticipant = inMeetingViewModel.speakerParticipant.value
            val isSpeaker =
                speakerParticipant != null && speakerParticipant.peerId == it.peerId && speakerParticipant.clientId == it.clientId

            inMeetingViewModel.onActivateVideo(it, isSpeaker)
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateRemoteResolutionOfSpeaker(isHiRes, session)
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

        bottomFloatingPanelViewHolder.updateRemoteAudioVideo(session)
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

        inMeetingViewModel.checkParticipantsResolution(status)
    }

    /**
     * Method that checks if a participant's name has changed and updates the UI
     *
     * @param peerId user handle that has changed
     */
    private fun updateParticipantName(peerId: Long) {
        logDebug("Participant's name has changed")
        val listParticipants = inMeetingViewModel.updateParticipantsName(peerId)
        if (listParticipants.isNotEmpty()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateName(listParticipants)
                }
            }
            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateName(listParticipants)
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
                CallUtil.openMeetingInProgress(requireContext(), anotherCall.chatid, false)
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
            inMeetingViewModel.leaveMeeting()
            checkIfAnotherCallShouldBeShown()
        } else if (inMeetingViewModel.shouldAssignModerator()) {
            EndMeetingBottomSheetDialogFragment.newInstance(inMeetingViewModel.getChatId())
                .run {
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

    private val showAssignModeratorFragment = fun() {
        val callback = fun() {
            leaveMeeting()
        }
        AssignModeratorBottomFragment.newInstance(callback).let {
            it.show(childFragmentManager, it.tag)
        }
    }

    /**
     * Dialog for confirming leave meeting action
     */
    private fun askConfirmationEndMeetingForUser() {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).apply {
            setMessage(StringResourcesUtils.getString(R.string.title_end_meeting))
            setPositiveButton(R.string.general_ok) { _, _ -> leaveMeeting() }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    /**
     * Method to control when I leave the call
     */
    private fun leaveMeeting() {
        inMeetingViewModel.leaveMeeting()
        if (inMeetingViewModel.amIAGuest()) {
            AccountController.logout(meetingActivity, MegaApplication.getInstance().megaApi)
        } else {
            checkIfAnotherCallShouldBeShown()
        }
    }

    /**
     * Method to control when call ended
     */
    private fun finishActivity() {
        logDebug("Finishing the activity")
        meetingActivity.snackbar?.dismiss()
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

        if (meetinglink.isEmpty()) {
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
            putExtra(Intent.EXTRA_TEXT, meetinglink)
            type = "text/plain"
        })
    }

    /**
     * Open invite participant page
     */
    override fun onInviteParticipants() {
        logDebug("chooseAddContactDialog")
        val inviteParticipantIntent =
            Intent(meetingActivity, AddContactActivityLollipop::class.java).apply {
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
        bInviteSent = false;
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
                status == TYPE_IN_SPEAKER_VIEW,
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
        const val WAITING_TIME = 26000L // 26 seconds
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
        checkAnotherCall()
    }

    override fun onCallStarted(chatId: Long, enableVideo: Boolean, enableAudio: Int) {
        logDebug("Call started")
        checkCallStarted(chatId)
    }

    override fun onDestroy() {
        super.onDestroy()

        removeUI()
        logDebug("Fragment destroyed")
        CallUtil.activateChrono(false, meetingChrono, null)
        MegaApplication.getInstance().unregisterProximitySensor()
        resumeAudioPlayerIfNotInCall(meetingActivity)
        RunOnUIThreadUtils.stop()
        bottomFloatingPanelViewHolder.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        MegaApplication.getInstance().startProximitySensor()
        checkChildFragments()
    }

    override fun onCallAnswered(chatId: Long, flag: Boolean) {
        if (chatId == inMeetingViewModel.getChatId()) {
            logDebug("Call answered")
            MegaApplication.getChatManagement().setSpeakerStatus(chatId, true)
            checkCallStarted(chatId)
        }
    }

    override fun onErrorAnsweredCall(errorCode: Int) {
        logDebug("Error answering the meeting so close it: $errorCode")
        showSnackbar(
            SNACKBAR_TYPE,
            StringResourcesUtils.getString(
                if (errorCode == MegaChatError.ERROR_TOOMANY) R.string.call_error_too_many_participants
                else R.string.call_error
            ),
            MEGACHAT_INVALID_HANDLE
        )

        finishActivity()
    }

    /**
     * Method that updates the microphone and camera values
     */
    private fun updateMicAndCam() {
        camIsEnable = sharedModel.cameraLiveData.value!!
        micIsEnable = sharedModel.micLiveData.value!!
        bottomFloatingPanelViewHolder.updateCamIcon(camIsEnable)
        bottomFloatingPanelViewHolder.updateMicIcon(micIsEnable)
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
            inMeetingViewModel.setChatId(chatId)
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
            inMeetingViewModel.answerChatCall(
                camIsEnable,
                micIsEnable,
                AnswerChatCallListener(requireContext(), this)
            )
        }
    }

    override fun onErrorJoinedChat(chatId: Long, userHandle: Long, error: Int) {
        logDebug("Error joining the meeting so close it, error code is $error")
        finishActivity()
    }

    private fun updatePanelParticipantList(list: MutableList<Participant> = mutableListOf()) {
        bottomFloatingPanelViewHolder
            .setParticipants(
                list,
                inMeetingViewModel.getMyOwnInfo(
                    sharedModel.micLiveData.value ?: false,
                    sharedModel.cameraLiveData.value ?: false
                )
            )
    }

    /**
     * Perform the necessary actions when the call is over.
     * Check if exists another call in progress or on hold
     */
    private fun checkIfAnotherCallShouldBeShown() {
        val anotherCall = inMeetingViewModel.getAnotherCall()
        if (anotherCall == null) {
            logDebug("Finish current call")
            finishActivity()
        } else {
            logDebug("Show another call")
            CallUtil.openMeetingInProgress(requireContext(), anotherCall.chatid, false)
            finishActivity()
        }
    }

    /**
     * The dialog for alerting the meeting is failed to created
     */
    private fun showMeetingFailedDialog() {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).apply {
            setMessage(StringResourcesUtils.getString(R.string.meeting_is_failed_content))
            setCancelable(false)
            setPositiveButton(R.string.general_ok) { _, _ ->
                MegaApplication.getInstance().removeRTCAudioManager()
                finishActivity()
            }
            show()
        }
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
}