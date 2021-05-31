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
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.meeting.listeners.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    StartChatCallListener.OnCallStartedCallback, AnswerChatCallListener.OnCallAnsweredCallback,
    AutoJoinPublicChatListener.OnJoinedChatCallback {

    val args: InMeetingFragmentArgs by navArgs()

    // Views
    lateinit var toolbar: MaterialToolbar

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null

    private lateinit var bannerAnotherCallLayout: View
    private var bannerAnotherCallTitle: EmojiTextView? = null
    private var bannerAnotherCallSubtitle: TextView? = null

    private lateinit var bannerInfoLayout: View
    private var bannerText: TextView? = null
    private var bannerIcon: ImageView? = null

    private var bannerParticipant: TextView? = null

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

    private val proximitySensorChangeObserver = Observer<Boolean> {
        val chatId = inMeetingViewModel.getChatId()
        when {
            chatId != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null -> {
                val realStatus = MegaApplication.getVideoStatus(chatId)
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
    }

    private val errorStatingCallObserver = Observer<Long> {
        when {
            inMeetingViewModel.isSameChatRoom(it) -> {
                logError("Error starting a call")
                MegaApplication.getInstance().removeRTCAudioManager()
                meetingActivity.finish()
            }
        }
    }

    private val nameChangeObserver = Observer<Long> { peerId ->
        when {
            peerId != MegaApiJava.INVALID_HANDLE -> {
                logDebug("Change in name")
                updateParticipantName(peerId)
            }
        }
    }

    private val noOutgoingCallObserver = Observer<Long> { callId ->
        when {
            inMeetingViewModel.isSameCall(callId) -> {
                val call = inMeetingViewModel.getCall()
                call?.let { chatCall ->
                    logDebug("The call is no longer an outgoing call")
                    updateToolbarSubtitle(chatCall)
                    enableOnHoldFab(chatCall.isOnHold)
                }
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
                    when {
                        item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV) -> {
                            logDebug("Change in my privileges")
                            if (MegaChatRoom.PRIV_MODERATOR == inMeetingViewModel.getOwnPrivileges()) {
                                showFixedBanner(
                                    megaChatApi.myUserHandle,
                                    TYPE_OWN_PRIVILEGE
                                )
                            }
                            bottomFloatingPanelViewHolder.updatePrivilege(inMeetingViewModel.getOwnPrivileges())
                        }
                        item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS) -> {
                            logDebug("Change in the privileges of a participant")
                            updateRemotePrivileges(inMeetingViewModel.updateParticipantsPrivileges())
                            bottomFloatingPanelViewHolder.updateRemotePrivileges(
                                inMeetingViewModel.updateParticipantsPrivileges()
                            )
                        }
                    }
                }
            }
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
        if (it.status != INVALID_CALL_STATUS) {
            if (inMeetingViewModel.isSameCall(it.callId)) {
                updateToolbarSubtitle(it)
                when (it.status) {
                    MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED -> finishActivity()
                }
            } else {
                checkAnotherCall()
            }
        }
    }

    private val callCompositionObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) &&
            it.status != INVALID_CALL_STATUS &&
            (it.callCompositionChange == 1 || it.callCompositionChange == -1)
        ) {
            logDebug("Change in call composition, review the UI")
            when {
                inMeetingViewModel.isOneToOneCall() -> {
                    if (it.numParticipants == 1 || it.numParticipants == 2) {
                        checkChildFragments()
                    }
                }
                else -> {
                    if (it.status == MegaChatCall.CALL_STATUS_IN_PROGRESS && !inMeetingViewModel.isRequestSent()) {
                        showFixedBanner(
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
        when {
            inMeetingViewModel.isSameCall(it.callId) -> {
                logDebug("Change in call on hold status")
                isCallOnHold(it.isOnHold)
                checkSwapCameraMenuItemVisibility()
            }
            else -> {
                checkAnotherCall()
            }
        }
    }

    private val localNetworkQualityObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            logDebug("Change in the network quality")
            showFixedBanner(MEGACHAT_INVALID_HANDLE, TYPE_NETWORK_QUALITY)
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showBannerInfo()
                val call = inMeetingViewModel.getCall()
                call?.let {
                    logDebug("Change in session on hold status")
                    when {
                        inMeetingViewModel.isOneToOneCall() -> {
                            if (it.hasLocalVideo() && callAndSession.second.isOnHold) {
                                sharedModel.clickCamera(false)
                            }
                        }
                        else -> {
                            updateOnHoldRemote(callAndSession.second)
                        }
                    }
                }
            } else {
                checkAnotherCall()
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                when {
                    !inMeetingViewModel.isOneToOneCall() -> {
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
                    }
                    else -> checkChildFragments()
                }
            } else {
                checkAnotherCall()
            }
        }

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                showBannerInfo()
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

        initToolbar()
        initFloatingWindowContainerDragListener(view)
        initFloatingPanel()

        val chatId: Long? =
            arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)

        chatId?.let {
            if (it != MEGACHAT_INVALID_HANDLE) {
                if (it == inMeetingViewModel.getChatId()) {
                    logDebug("Same call")
                } else {
                    logDebug("Different call")
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
        isAudioEnable?.let {
            when (it) {
                true -> {
                    sharedModel.micInitiallyOn()
                }
            }
        }

        val isVideoEnable: Boolean? =
            arguments?.getBoolean(MeetingActivity.MEETING_VIDEO_ENABLE, false)
        isVideoEnable?.let {
            when (it) {
                true -> {
                    sharedModel.camInitiallyOn()
                }
            }
        }

        val currentCall: MegaChatCall? = inMeetingViewModel.getCall()

        if (currentCall != null) {
            when {
                currentCall.hasLocalAudio() -> sharedModel.micInitiallyOn()
                currentCall.hasLocalVideo() -> sharedModel.camInitiallyOn()
            }
            updateToolbarSubtitle(currentCall)
            enableOnHoldFab(currentCall.isOnHold)
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
        inMeetingViewModel.getCall()?.let {
            isCallOnHold(inMeetingViewModel.isCallOnHold())
        }
    }

    private fun takeActionByArgs() {
        when (args.action) {
            MEETING_ACTION_CREATE -> initStartMeeting()
            MEETING_ACTION_JOIN -> {
                inMeetingViewModel.joinPublicChat(
                    args.chatId,
                    AutoJoinPublicChatListener(requireContext(), this)
                )
            }
            MEETING_ACTION_GUEST -> {
                inMeetingViewModel.chatLogout(ChatRequestListener(onSuccess = { _, _, _ ->
                    inMeetingViewModel.createEphemeralAccountAndJoinChat(
                        args.firstName,
                        args.lastName,
                        MegaRequestListener(onSuccess = { _, _, _ ->
                            inMeetingViewModel.fetchNodes(MegaRequestListener(onSuccess = { _, _, _ ->

                                inMeetingViewModel.chatConnect(ChatRequestListener(onSuccess = { _, _, _ ->
                                    inMeetingViewModel.openChatPreview(
                                        meetinglink,
                                        ChatRequestListener(onSuccess = { _, request, _ ->
                                            logDebug(
                                                "Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Call id: ${
                                                    request.megaHandleList?.get(
                                                        0
                                                    )
                                                }"
                                            )

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
                            }))
                        })
                    )
                }))
            }
            MEETING_ACTION_RINGING_VIDEO_ON -> {
                sharedModel.micInitiallyOn()
                sharedModel.camInitiallyOn()
            }
            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                sharedModel.micInitiallyOn()
            }
            MEETING_ACTION_START -> {
                onJoinedChat(arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)!!, MEGACHAT_INVALID_HANDLE)
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
    }

    /**
     * Observe the Orientation changes and Update the layout for landscape and portrait screen
     *
     * @param newConfig
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

        floatingWindowFragment?.updateOrientation(
            newConfig.orientation,
        )

        gridViewCallFragment?.updateLayout(
            newConfig.orientation,
            outMetrics.widthPixels,
            outMetrics.heightPixels
        )
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(EVENT_PROXIMITY_SENSOR_CHANGE, Boolean::class.java)
            .observeSticky(this, proximitySensorChangeObserver)

        LiveEventBus.get(EVENT_ERROR_STARTING_CALL, Long::class.java)
            .observeSticky(this, errorStatingCallObserver)

        LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observeSticky(this, noOutgoingCallObserver)

        LiveEventBus.get(EVENT_CONTACT_NAME_CHANGE, Long::class.java)
            .observeSticky(this, nameChangeObserver)

        LiveEventBus.get(EVENT_PRIVILEGES_CHANGE, MegaChatListItem::class.java)
            .observeSticky(this, privilegesChangeObserver)

        LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long::class.java)
            .observeSticky(this, visibilityChangeObserver)

        //Calls level
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callStatusObserver)

        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callCompositionObserver)

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, callOnHoldObserver)

        LiveEventBus.get(EVENT_LOCAL_NETWORK_QUALITY_CHANGE, MegaChatCall::class.java)
            .observeSticky(this, localNetworkQualityObserver)

        //Sessions Level
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .observeSticky(this, sessionStatusObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observeSticky(this, sessionOnHoldObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_HIRES_CHANGE)
            .observeSticky(this, sessionHiResObserver as Observer<Any>)
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_SESSION_ON_LOWRES_CHANGE)
            .observeSticky(this, sessionLowResObserver as Observer<Any>)
    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbarTitle = meetingActivity.title_toolbar
        toolbarSubtitle = meetingActivity.subtitle_toolbar
        toolbarSubtitle?.let {
            it.text = StringResourcesUtils.getString(R.string.chat_connecting)
        }

        toolbar.setOnClickListener { if (!inMeetingViewModel.isOneToOneCall()) showMeetingInfoFragment() }
        bannerAnotherCallLayout = meetingActivity.banner_another_call
        bannerAnotherCallTitle = meetingActivity.banner_another_call_title
        bannerAnotherCallSubtitle = meetingActivity.banner_another_call_subtitle
        bannerParticipant = meetingActivity.banner_participant
        bannerInfoLayout = meetingActivity.banner_info
        bannerIcon = meetingActivity.banner_icon
        bannerText = meetingActivity.banner_text
        meetingChrono = meetingActivity.simple_chronometer

        meetingActivity.setSupportActionBar(toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
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
                bottomFloatingPanelViewHolder.updateMeetingType(!inMeetingViewModel.isOneToOneCall())
            }
        }

        sharedModel.meetingLinkLiveData.observe(viewLifecycleOwner) {
            when {
                !it.isNullOrEmpty() -> {
                    logDebug("Link has changed")
                    meetinglink = it
                    when {
                        inMeetingViewModel.isWaitingForLink() -> {
                            inMeetingViewModel.setWaitingForLink(false)
                            shareLink()
                        }
                        else -> {
                            inMeetingViewModel.startMeeting(
                                micIsEnable,
                                camIsEnable,
                                StartChatCallListener(meetingActivity, this, this)
                            )
                        }
                    }
                }
            }
        }

        sharedModel.meetingNameLiveData.observe(viewLifecycleOwner) {
            when {
                !TextUtil.isTextEmpty(it) -> {
                    logDebug("Meeting name has changed")
                    inMeetingViewModel.setTitleChat(it)
                }
            }
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            when {
                toolbarTitle != null -> {
                    logDebug("Chat title has changed")
                    toolbarTitle?.text = it
                }
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            when {
                micIsEnable != it -> {
                    logDebug("Mic status has changed to $it")
                    micIsEnable = it
                    updateLocalAudio(it)
                }
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            when {
                camIsEnable != it -> {
                    logDebug("Camera status has changed to $it")
                    camIsEnable = it
                    updateLocalVideo(it)
                }
            }
        }

        sharedModel.speakerLiveData.observe(viewLifecycleOwner) {
            logDebug("Speaker status has changed to $it")
            updateSpeaker(it)
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            when {
                it -> {
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
                            }
                        }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                        .build().launch(false)
                }
            }
        }
        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            when {
                it -> {
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
                            }
                        }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                        .build().launch(false)
                }
            }
        }

        sharedModel.snackBarLiveData.observe(viewLifecycleOwner) {
            (requireActivity() as MeetingActivity).showSnackbar(it)
        }
    }

    private fun onAudioNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    showRequestPermissionSnackBar()
                }
            }
        }
    }

    private fun onCameraNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    showRequestPermissionSnackBar()
                }
            }
        }
    }

    private fun initFloatingWindowContainerDragListener(view: View) {
        dragTouchListener = OnDragTouchListener(
            floatingWindowContainer,
            view,
            object :
                OnDragTouchListener.OnDragActionListener {

                override fun onDragStart(view: View?) {
                    if (toolbar.isVisible) {
                        val maxTop =
                            if (bannerInfoLayout.isVisible) bannerInfoLayout.bottom else toolbar.bottom
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
        // If the tips is showing, can not hide the toolbar and panel
        if (bottomFloatingPanelViewHolder.isPopWindowShowing()) return

        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < TAP_THRESHOLD) return

        toolbar.fadeInOut(dy = TOOLBAR_DY, toTop = true)

        if (bannerShouldBeShown) {
            bannerInfoLayout.fadeInOut(dy = FLOATING_BOTTOM_SHEET_DY, toTop = true)
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

    private fun checkRelativePositionWithToolbar() {
        val maxTop = if (bannerInfoLayout.isVisible) bannerInfoLayout.bottom else toolbar.bottom

        val isIntersect = (maxTop - floatingWindowContainer.y) > 0
        if (toolbar.isVisible && isIntersect) {
            floatingWindowContainer.moveY(maxTop.toFloat())
        }

        val isIntersectPreviously = (maxTop - previousY) > 0
        if (!toolbar.isVisible && isIntersectPreviously && previousY >= 0) {
            floatingWindowContainer.moveY(previousY)
        }
    }

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
            when {
                isOnHold -> {
                    it.text = StringResourcesUtils.getString(R.string.call_on_hold)
                }
                else -> {
                    it.text = StringResourcesUtils.getString(R.string.call_in_progress_layout)
                }
            }
            it.alpha = 1f
        }

        bannerAnotherCallLayout.let {
            when {
                isOnHold -> {
                    it.alpha = 0.9f
                }
                else -> {
                    it.alpha = 1f
                }
            }
            it.isVisible = true
        }
    }

    /**
     * Show the correct UI in a meeting
     */
    private fun updateGroupUI() {
        logDebug("Update group UI")
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
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
        val call: MegaChatCall = inMeetingViewModel.getCall() ?: return

        logDebug("Check child fragments")

        if (call.status >= MegaChatCall.CALL_STATUS_JOINING) {
            when {
                inMeetingViewModel.isOneToOneCall() -> {
                    logDebug("One to one call")
                    updateOneToOneUI()
                }
                else -> {
                    logDebug("Group call")
                    updateGroupUI()
                }
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

        status = TYPE_IN_ONE_TO_ONE
        logDebug("One to One call")
        val call: MegaChatCall? = inMeetingViewModel.getCall()
        call?.let { currentCall ->
            val session = inMeetingViewModel.getSessionOneToOneCall(currentCall)
            session?.let { userSession ->
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
     */
    private fun waitingForConnection(chatId: Long) {
        if (status == TYPE_WAITING_CONNECTION) return

        logDebug("Waiting for connection call UI")
        status = TYPE_WAITING_CONNECTION

        individualCallFragment = IndividualCallFragment.newInstance(
            chatId,
            megaChatApi.myUserHandle,
            false
        )

        individualCallFragment?.let {
            loadChildFragment(
                R.id.meeting_container,
                it,
                IndividualCallFragment.TAG
            )
        }

        floatingWindowFragment?.let {
            removeChildFragment(it)
            floatingWindowFragment = null
        }

        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Show local fragment UI
     *
     * @param chatId
     */
    private fun initLocal(chatId: Long) {
        logDebug("Init local fragment")
        if (floatingWindowFragment == null) {
            floatingWindowFragment = IndividualCallFragment.newInstance(
                chatId,
                megaChatApi.myUserHandle,
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

        status = TYPE_IN_SPEAKER_VIEW
        logDebug("Group call - Speaker View")

        gridViewCallFragment?.let {
            removeChildFragment(it)
            gridViewCallFragment = null
        }

        if (speakerViewCallFragment == null) {
            speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
        }

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

        status = TYPE_IN_GRID_VIEW
        logDebug("Group call - Grid View")

        speakerViewCallFragment?.let {
            removeChildFragment(it)
            speakerViewCallFragment = null
        }

        if (gridViewCallFragment == null) {
            gridViewCallFragment = GridViewCallFragment.newInstance()
        }

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
     * @param chatId
     */
    private fun initGroupCall(chatId: Long) {
        if (status != TYPE_IN_GRID_VIEW && status != TYPE_IN_SPEAKER_VIEW) {
            initLocal(chatId)
        }

        if (!isManualModeView) {
            inMeetingViewModel.getCall()?.let {
                if (it.numParticipants <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC) {
                    logDebug("Automatic mode - Grid view")
                    initGridViewMode()
                } else {
                    logDebug("Automatic mode - Speaker view")
                    initSpeakerViewMode()
                }
            }
        } else {
            if (status == TYPE_IN_SPEAKER_VIEW) {
                logDebug("Manual mode - Speaker view")
                initSpeakerViewMode()
            } else {
                logDebug("Manual mode - Grid view")
                initGridViewMode()
            }
        }
    }

    /**
     * Method controlling whether to initiate a call
     */
    private fun initStartMeeting() {
        when (sharedModel.currentChatId.value) {
            MEGACHAT_INVALID_HANDLE -> {
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
                    micIsEnable,
                    camIsEnable,
                    StartChatCallListener(requireContext(), this, this)
                )
            }
        }
    }

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
            MegaChatCall.CALL_STATUS_JOINING, MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                if (inMeetingViewModel.isRequestSent() && !MegaApplication.isCreatingMeeting(
                        inMeetingViewModel.getChatId()
                    )
                ) {
                    CallUtil.activateChrono(false, meetingChrono, null)
                    toolbarSubtitle?.let {
                        it.text = StringResourcesUtils.getString(R.string.outgoing_call_starting)
                    }
                } else {
                    toolbarSubtitle?.let {
                        it.text = StringResourcesUtils.getString(R.string.duration_meeting)
                    }
                    CallUtil.activateChrono(true, meetingChrono, call)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)
        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
        swapCameraMenuItem = menu.findItem(R.id.swap_camera)

        checkGridSpeakerViewMenuItemVisibility()
        checkSwapCameraMenuItemVisibility()
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
        meetingActivity.showSnackbar(type, binding.root, content, chatId)
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

        /**
         * Observer the participant List
         */
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
                updatePanelParticipantList(it.toMutableList())
            }
        }

        bottomFloatingPanelViewHolder.propertyUpdaters.add {
            toolbar.alpha = 1 - it
            // When the bottom on the top, will set the toolbar invisible, otherwise will cover the scroll event of the panel
            toolbar.isVisible = it != 1.0f

            when {
                bannerInfoLayout.isVisible -> {
                    bannerInfoLayout.alpha = 1 - it
                }
            }
            when {
                bannerAnotherCallLayout.isVisible -> {
                    bannerAnotherCallLayout.alpha = 1 - it
                }
            }
        }
    }

    /**
     * Change Mic State
     */
    override fun onChangeMicState(micOn: Boolean) {
        sharedModel.clickMic(!micOn)
    }

    private fun updateLocalAudio(micOn: Boolean) {
        bottomFloatingPanelViewHolder.updateMicIcon(micOn)
        showBannerInfo()
    }

    /**
     * Method that controls whether the fixed banner should be displayed.
     * This banner is displayed when a participant joins/leaves the meeting
     * or when my network quality is low.
     */
    private fun showFixedBanner(peerId: Long, type: Int) {
        bannerParticipant?.let {
            when {
                inMeetingViewModel.showBannerFixedBanner(it, peerId, type) &&
                        bottomFloatingPanelViewHolder.getState() != BottomSheetBehavior.STATE_EXPANDED -> {
                    it.alpha = 1f
                    logDebug("Show fixed banner")
                    it.isVisible = true
                    if (type != TYPE_NETWORK_QUALITY) {
                        it.animate()?.alpha(0f)?.duration = INFO_ANIMATION.toLong()
                    }
                }
                else -> {
                    logDebug("Hide fixed banner")
                    it.isVisible = false
                }
            }
        }
    }

    /**
     * Method that controls whether the banner should be displayed.
     * This banner is displayed when the call is muted
     * or if the session or call is on hold.
     */
    private fun showBannerInfo() {
        bannerInfoLayout.let {
            bannerShouldBeShown = inMeetingViewModel.showAppropriateBanner(
                bannerIcon,
                bannerText
            )

            when {
                bannerShouldBeShown -> {
                    it.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.gradient_shape_callschat
                    )
                    logDebug("Show banner info")
                    it.isVisible = true
                }
                else -> {
                    logDebug("Hide banner info")
                    it.isVisible = false
                }
            }
            if (bottomFloatingPanelViewHolder.getState() == BottomSheetBehavior.STATE_EXPANDED || !toolbar.isVisible) {
                when {
                    bannerInfoLayout.isVisible -> {
                        bannerInfoLayout.alpha = 0f
                    }
                }
            }

            // Delay a bit to wait for 'bannerInfoLayout' finish layouting, otherwise, its bottom is 0.
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
            bottomFloatingPanelViewHolder.updateHoldIcon(isHold)
        }

        showBannerInfo()
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
     */
    override fun onChangeCamState(camOn: Boolean) {
        sharedModel.clickCamera(!camOn)
    }

    /**
     * Method that checks if the local video has changed in one to one call and updates the UI.
     */
    private fun controlVideoLocalOneToOneCall(camOn: Boolean) {
        individualCallFragment?.let {
            if (it.isAdded) {
                when {
                    camOn -> {
                        it.checkVideoOn(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    }
                    else -> {
                        it.videoOffUI(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    }
                }
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                when {
                    camOn -> {
                        it.checkVideoOn(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    }
                    else -> {
                        it.videoOffUI(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    }
                }
            }
        }
    }

    /**
     * Method that checks if the local video has changed and updates the UI.
     */
    private fun updateLocalVideo(camOn: Boolean) {
        logDebug("Local audio or video changes")
        inMeetingViewModel.getCall()?.let {
            val isVideoOn: Boolean = it.hasLocalVideo()
            when {
                !inTemporaryState -> {
                    MegaApplication.setVideoStatus(it.chatid, isVideoOn)
                }
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(camOn)
        checkSwapCameraMenuItemVisibility()
        controlVideoLocalOneToOneCall(camOn)
    }

    /**
     * Method that checks if the session's on hold state has changed and updates the UI
     *
     * @param session
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
     * @param session
     */
    private fun updateLowOrHiResolution(isHiRes: Boolean, session: MegaChatSession) {
        logDebug("The resolution of a participant needs to be updated")
        inMeetingViewModel.getParticipant(session.peerid, session.clientid)?.let {
            val speakerParticipant = inMeetingViewModel.speakerParticipant.value
            if (speakerParticipant != null && speakerParticipant.peerId == it.peerId && speakerParticipant.clientId == it.clientId) {
                inMeetingViewModel.onActivateVideo(it, true)
            } else {
                inMeetingViewModel.onActivateVideo(it, false)
            }
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
                when {
                    isAudioChange -> it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                    isVideoChange -> it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                when {
                    isAudioChange -> it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                    isVideoChange -> it.updateRemoteAudioVideo(TYPE_VIDEO, session)
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
        if (anotherCall == null) {
            logDebug("No other calls in progress")
            inMeetingViewModel.setCallOnHold(isHold)
        } else {
            if (anotherCall.isOnHold) {
                logDebug("Change of status on hold and switch of call")
                inMeetingViewModel.setCallOnHold(true)
                inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, false)
                CallUtil.openMeetingInProgress(requireContext(), anotherCall.chatid, false)

            } else {
                if (inMeetingViewModel.isCallOnHold()) {
                    logDebug("Change of status on hold")
                    inMeetingViewModel.setCallOnHold(false)
                    inMeetingViewModel.setAnotherCallOnHold(anotherCall.chatid, true)
                } else {
                    logDebug("The current call is not on hold, change the status")
                    inMeetingViewModel.setCallOnHold(isHold)
                }
            }
        }
    }

    /**
     * Change Speaker state
     */
    override fun onChangeSpeakerState() {
        sharedModel.clickSpeaker()
    }

    private fun updateSpeaker(device: AppRTCAudioManager.AudioDevice) {
        bottomFloatingPanelViewHolder.updateSpeakerIcon(device)
    }

    /**
     * Pop up dialog for end meeting for the user/guest
     *
     * Will show bottom sheet fragment for the moderator
     */
    override fun onEndMeeting() {
        when {
            inMeetingViewModel.isOneToOneCall() -> {
                logDebug("End the one to one call")
                inMeetingViewModel.leaveMeeting()
                checkIfAnotherCallShouldBeShown()
            }
            inMeetingViewModel.shouldAssignModerator() -> {
                EndMeetingBottomSheetDialogFragment.newInstance(inMeetingViewModel.getChatId())
                    .run {
                        setAssignCallBack(showAssignModeratorFragment)
                        show(
                            this@InMeetingFragment.childFragmentManager,
                            tag
                        )
                    }
            }
            else ->
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
            setMessage(getString(R.string.title_end_meeting))
            setPositiveButton(R.string.general_ok) { _, _ -> leaveMeeting() }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    private fun leaveMeeting() {
        when {
            inMeetingViewModel.isGuest() -> {
                meetingActivity.startActivity(
                    Intent(
                        meetingActivity,
                        LeftMeetingActivity::class.java
                    )
                )
                meetingActivity.finish()
            }
            else -> {
                inMeetingViewModel.leaveMeeting()
                checkIfAnotherCallShouldBeShown()
            }
        }
    }

    /**
     * Method to control when call ended
     */
    private fun finishActivity() {
        logDebug("Finishing the activity")
        if (inMeetingViewModel.getChatId() != MEGACHAT_INVALID_HANDLE) {
            MegaApplication.setCreatingMeeting(inMeetingViewModel.getChatId(), false)
        }
        meetingActivity.finish()
    }

    /**
     * Send share link
     */
    override fun onShareLink() {
        if (inMeetingViewModel.isOneToOneCall() || !inMeetingViewModel.isChatRoomPublic()) {
            logError("Error getting the link, it is a private chat")
            return
        }

        if (meetinglink.isEmpty()) {
            inMeetingViewModel.setWaitingForLink(true)
            sharedModel.createChatLink(
                inMeetingViewModel.getChatId(),
                inMeetingViewModel.isModerator()
            )
            logError("Error, the link doesn't exist")
            return
        }

        shareLink()
    }

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
                putExtra("contactType", CONTACT_TYPE_MEGA)
                putExtra("chat", true)
                putExtra("chatId", 123L)
                putExtra("aBtitle", getString(R.string.invite_participants))
            }
        meetingActivity.startActivityForResult(
            inviteParticipantIntent, REQUEST_ADD_PARTICIPANTS
        )
    }

    /**
     * Show participant bottom sheet when user click the three dots on participant item
     */
    override fun onParticipantOption(participant: Participant) {
        val participantBottomSheet =
            MeetingParticipantBottomSheetDialogFragment.newInstance(
                inMeetingViewModel.isGuest(),
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
    }

    private fun checkCallStarted(chatId: Long) {
        MegaApplication.getInstance().openCallService(chatId)
        inMeetingViewModel.setCall(chatId)
        checkChildFragments()
        showBannerInfo()
        checkAnotherCall()
    }

    override fun onCallStarted(chatId: Long, enableVideo: Boolean, enableAudio: Int) {
        logDebug("Call started")
        MegaApplication.setCreatingMeeting(chatId, true)
        checkCallStarted(chatId)
    }

    override fun onDestroy() {
        logDebug("Fragment destroyed")
        CallUtil.activateChrono(false, meetingChrono, null)
        resumeAudioPlayerIfNotInCall(meetingActivity)
        RunOnUIThreadUtils.stop()
        super.onDestroy()
    }

    override fun onCallAnswered(chatId: Long, flag: Boolean) {
        if (chatId == inMeetingViewModel.getChatId()) {
            logDebug("Call answered")
            MegaApplication.setSpeakerStatus(chatId, true)
            MegaApplication.setOpeningMeetingLink(chatId, false)
            checkCallStarted(chatId)
        }
    }

    override fun onErrorAnsweredCall(errorCode: Int) {
        logDebug("Error answering the meeting so close it: $errorCode")
        if (errorCode == MegaChatError.ERROR_TOOMANY) {
            showSnackbar(
                SNACKBAR_TYPE,
                StringResourcesUtils.getString(R.string.call_error_too_many_participants),
                -1
            )
        } else {
            showSnackbar(
                SNACKBAR_TYPE,
                StringResourcesUtils.getString(R.string.call_error),
                -1
            )
        }

        MegaApplication.setOpeningMeetingLink(args.chatId, false)
        finishActivity()
    }

    override fun onJoinedChat(chatId: Long, userHandle: Long) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            logDebug("Update chat id $chatId")
            sharedModel.updateChatRoomId(chatId)
            inMeetingViewModel.setChatId(chatId)
        }

        inMeetingViewModel.checkAnotherCallsInProgress(chatId)

        if (args.action == MEETING_ACTION_GUEST) {
            inMeetingViewModel.registerConnectionUpdateListener(args.chatId) {
                answerCallAfterJoin()
            }
        } else {
            answerCallAfterJoin()
        }
    }

    private fun answerCallAfterJoin() {
        inMeetingViewModel.getCall()?.let {
            logDebug("Joined to chat, answer call")
            inMeetingViewModel.answerChatCall(
                camIsEnable,
                micIsEnable,
                AnswerChatCallListener(requireContext(), this)
            )
        }
    }

    override fun onErrorJoinedChat(chatId: Long) {
        logDebug("Error joining the meeting so close it")
        MegaApplication.setOpeningMeetingLink(args.chatId, false)
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
     */
    private fun checkIfAnotherCallShouldBeShown() {
        //Check if exists another call in progress or on hold
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

    class ChatRequestListener(
        private val onSuccess: (
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError
        ) -> Unit,
        private val onFail: (
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError
        ) -> Unit = { _, request, e ->
            logWarning("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")
        },
        private val isSuccess: (
            request: MegaChatRequest,
            e: MegaChatError
        ) -> Boolean = { _, e ->
            e.errorCode == MegaChatError.ERROR_OK
        }
    ) : MegaChatRequestListenerInterface {

        override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {
            logDebug("Start [${request.requestString}]")
        }

        override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest) {

        }

        override fun onRequestFinish(
            api: MegaChatApiJava,
            request: MegaChatRequest,
            e: MegaChatError
        ) {
            if (isSuccess(request, e)) {
                logDebug("[${request.requestString}] -> is successful")
                onSuccess(api, request, e)
            } else {
                onFail(api, request, e)
            }
        }

        override fun onRequestTemporaryError(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?
        ) {

        }
    }

    class MegaRequestListener(
        private val onSuccess: (
            api: MegaApiJava,
            request: MegaRequest,
            e: MegaError
        ) -> Unit,
        private val onFail: (
            api: MegaApiJava,
            request: MegaRequest,
            e: MegaError
        ) -> Unit = { _, request, e ->
            logWarning("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")
        },
        private val isSuccess: (
            request: MegaRequest,
            e: MegaError
        ) -> Boolean = { _, e ->
            e.errorCode == MegaError.API_OK
        }
    ) : MegaRequestListenerInterface {

        override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
            logDebug("Start [${request.requestString}]")
        }

        override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {

        }

        override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
            if (isSuccess(request, e)) {
                logDebug("[${request.requestString}] -> is successful")
                onSuccess(api, request, e)
            } else {
                onFail(api, request, e)
            }
        }

        override fun onRequestTemporaryError(
            api: MegaApiJava,
            request: MegaRequest,
            e: MegaError?
        ) {
        }
    }
}