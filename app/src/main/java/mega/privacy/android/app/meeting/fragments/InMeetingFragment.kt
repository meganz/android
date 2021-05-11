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
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.AutoJoinPublicChatListener
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.OnDragTouchListener
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom.CHANGE_TYPE_OWN_PRIV
import mega.privacy.android.app.utils.ChatUtil.*
import mega.privacy.android.app.utils.permission.permissionsBuilder

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

    // Flags, should get the value from somewhere
    private var isGuest = false
    private var isModerator = true

    private var status = NOT_TYPE
    private val MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC = 6

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
                        inTemporaryState = true
                        sharedModel.clickCamera(false)
                    }
                    else -> {
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
                MegaApplication.getInstance().removeRTCAudioManager()
                meetingActivity.finish()
            }
        }
    }

    private val nameChangeObserver = Observer<Long> { peerId ->
        when {
            peerId != MegaApiJava.INVALID_HANDLE -> {
                updateParticipantName(peerId)
            }
        }
    }

    private val noOutgoingCallObserver = Observer<Long> { callId ->
        when {
            inMeetingViewModel.isSameCall(callId) -> {
                val call = inMeetingViewModel.getCall()
                call?.let { chatCall ->
                    updateToolbarSubtitle(chatCall)
                    enableOnHoldFab(chatCall.isOnHold)
                }
            }
        }
    }

    private val visibilityChangeObserver = Observer<Long> {
        inMeetingViewModel.updateParticipantsVisibility(it)
    }

    private val privilegesChangeObserver = Observer<MegaChatListItem> {
        when {
            inMeetingViewModel.isSameChatRoom(it.chatId) -> {
                inMeetingViewModel.getCall()?.let {
                    when (it.status) {
                        MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                            when {
                                it.hasChanged(CHANGE_TYPE_OWN_PRIV) -> {
                                    when (MegaChatRoom.PRIV_MODERATOR) {
                                        inMeetingViewModel.getOwnPrivileges() -> {
                                            showFixedBanner(
                                                megaChatApi.myUserHandle,
                                                TYPE_OWN_PRIVILEGE
                                            )
                                        }
                                    }
                                    bottomFloatingPanelViewHolder.updatePrivilege(inMeetingViewModel.getOwnPrivileges())
                                }
                                it.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS) -> {
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
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
        when {
            it.status != INVALID_CALL_STATUS -> {
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
    }

    private val callCompositionObserver = Observer<MegaChatCall> {
        when {
            inMeetingViewModel.isSameCall(it.callId) &&
                    it.status != INVALID_CALL_STATUS &&
                    (it.callCompositionChange == 1 || it.callCompositionChange == -1) -> {

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
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        when {
            inMeetingViewModel.isSameCall(it.callId) -> {
                isCallOnHold(it.isOnHold)
                checkSwapCameraMenuItemVisibility()
            }
            else -> {
                checkAnotherCall()
            }
        }
    }

    private val localNetworkQualityObserver = Observer<MegaChatCall> {
        when {
            inMeetingViewModel.isSameCall(it.callId) -> {
                showFixedBanner(MEGACHAT_INVALID_HANDLE, TYPE_NETWORK_QUALITY)
            }
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    showBannerInfo()
                    val call = inMeetingViewModel.getCall()
                    call?.let {
                        when {
                            inMeetingViewModel.isOneToOneCall() -> {
                                when {
                                    it.hasLocalVideo() && callAndSession.second.isOnHold -> {
                                        sharedModel.clickCamera(false)
                                    }
                                }
                            }
                            else -> {
                                updateOnHoldRemote(callAndSession.second)
                            }
                        }
                    }
                }
                else -> {
                    checkAnotherCall()
                }
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    when {
                        !inMeetingViewModel.isOneToOneCall() -> {
                            when (callAndSession.second.status) {
                                MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                                    logDebug("Session in progress")

                                    val position =
                                        inMeetingViewModel.createParticipant(
                                            callAndSession.second,
                                            status
                                        )
                                    position?.let {
                                        if (position != INVALID_POSITION) {
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
                                            participantAddedOfLeftMeeting(false, it)
                                        }
                                    }
                                    if (!inMeetingViewModel.isOneToOneCall() && inMeetingViewModel.amIAloneOnTheCall(
                                            inMeetingViewModel.getChatId()
                                        )
                                    ) {
                                        checkChildFragments()
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    checkAnotherCall()
                }
            }
        }

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    showBannerInfo()

                    when {
                        !inMeetingViewModel.isOneToOneCall() -> {
                            val isAudioChange =
                                inMeetingViewModel.changesInRemoteAudioFlag(callAndSession.second)
                            val isVideoChange =
                                inMeetingViewModel.changesInRemoteVideoFlag(callAndSession.second)
                            updateRemoteAVFlags(callAndSession.second, isAudioChange, isVideoChange)
                        }
                    }
                }
            }
        }

    private val sessionLowResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) -> {
                    if (callAndSession.second.canRecvVideoLowRes()) {
                        logDebug("Can receive low-resolution video")
                    } else {
                        logDebug("Can not receive low-resolution video")
                        if (inMeetingViewModel.changesInLowRes(callAndSession.second)) {
                            updateLowOrHiResolution(false, callAndSession.second)
                        }
                    }
                }
            }
        }

    private val sessionHiResObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) -> {
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

        when {
            currentCall != null -> {
                when {
                    currentCall.hasLocalAudio() -> sharedModel.micInitiallyOn()
                    currentCall.hasLocalVideo() -> sharedModel.camInitiallyOn()
                }
                updateToolbarSubtitle(currentCall)
                enableOnHoldFab(currentCall.isOnHold)
            }
            else -> {
                enableOnHoldFab(false)
            }
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
                inMeetingViewModel.createEphemeralAccountAndJoinChat(
                    args.chatId,
                    args.firstName,
                    args.lastName
                )
            }
            MEETING_ACTION_RINGING_VIDEO_ON -> {
                sharedModel.micInitiallyOn()
                sharedModel.camInitiallyOn()
            }
            MEETING_ACTION_RINGING_VIDEO_OFF -> {
                sharedModel.micInitiallyOn()
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
        gridViewCallFragment?.let {
            it.updateLayout(
                newConfig.orientation,
                outMetrics.widthPixels,
                outMetrics.heightPixels
            )
        }
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
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE)
            .observeSticky(this, sessionStatusObserver as Observer<Any>)

        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE)
            .observeSticky(this, sessionOnHoldObserver as Observer<Any>)

        LiveEventBus.get(EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeSticky(this, remoteAVFlagsObserver as Observer<Any>)

        LiveEventBus.get(EVENT_SESSION_ON_HIRES_CHANGE)
            .observeSticky(this, sessionHiResObserver as Observer<Any>)

        LiveEventBus.get(EVENT_SESSION_ON_LOWRES_CHANGE)
            .observeSticky(this, sessionLowResObserver as Observer<Any>)
    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbarTitle = meetingActivity.title_toolbar
        toolbarSubtitle = meetingActivity.subtitle_toolbar
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

    /**
     * Init View Models
     */
    private fun initViewModel() {
        sharedModel.currentChatId.observe(viewLifecycleOwner) {
            it?.let {
                inMeetingViewModel.setChatId(it)
                bottomFloatingPanelViewHolder.updateMeetingType(!inMeetingViewModel.isOneToOneCall())
            }
        }

        sharedModel.meetingLinkLiveData.observe(viewLifecycleOwner) {
            when {
                !it.isNullOrEmpty() -> {
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
                    inMeetingViewModel.setTitleChat(it)
                }
            }
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            when {
                toolbarTitle != null -> {
                    toolbarTitle?.text = it
                }
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            when {
                micIsEnable != it -> {
                    micIsEnable = it
                    updateLocalAudio(it)
                }
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            when {
                camIsEnable != it -> {
                    camIsEnable = it
                    updateLocalVideo(it)
                }
            }
        }

        sharedModel.speakerLiveData.observe(viewLifecycleOwner) {
            updateSpeaker(it)
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            when {
                it -> {
                    permissionsBuilder(arrayOf(Manifest.permission.CAMERA).toCollection(ArrayList()))
                        .setOnRequiresPermission { l -> onRequiresCameraPermission(l) }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                        .build().launch(false)
                }
            }
        }
        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            when {
                it -> {
                    permissionsBuilder(arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(ArrayList()))
                        .setOnRequiresPermission { l -> onRequiresAudioPermission(l) }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                        .build().launch(false)
                }
            }
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
            when {
                inMeetingViewModel.amIAloneOnTheCall(it.chatid) -> {
                    logDebug("One to one call. Waiting for connection")
                    waitingForConnection(it.chatid)
                }
                else -> {
                    val session = inMeetingViewModel.getSessionOneToOneCall(it)
                    session?.let {
                        logDebug("One to one call. Session exists")
                        initOneToOneCall()
                    }
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
            updateOnHoldFabButton(anotherCall)
            updateBannerAnotherCall(anotherCall)
            return
        }

        bottomFloatingPanelViewHolder.let {
            it.changeOnHoldIconDrawable(false)
        }
        logDebug("No other calls in progress or on hold")
        bannerAnotherCallLayout.isVisible = false
    }

    /**
     * Update the button if there are calls on hold in other chats or not.
     *
     * @param anotherCall Another call on hold or in progress
     */
    private fun updateOnHoldFabButton(anotherCall: MegaChatCall) {
        bottomFloatingPanelViewHolder.let {
            it.changeOnHoldIcon(
                anotherCall.isOnHold
            )
        }
    }

    /**
     * Update the banner if there are calls on hold in other chats or not.
     */
    private fun updateBannerAnotherCall(anotherCall: MegaChatCall) {
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
            when {
                inMeetingViewModel.amIAloneOnTheCall(call.chatid) -> {
                    waitingForConnection(call.chatid)
                }
                else -> {
                    initGroupCall(call.chatid)
                }
            }
        }
    }

    private fun checkCurrentParticipants() {
        inMeetingViewModel.getCall()?.let {
            inMeetingViewModel.createCurrentParticipants(it.sessionsClientid, status)
        }
    }

    /**
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
        logDebug("Check child fragments")

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
    }

    /**
     * Show one to one call UI
     */
    private fun initOneToOneCall() {
        when (status) {
            TYPE_IN_ONE_TO_ONE -> return
            else -> {
                status = TYPE_IN_ONE_TO_ONE
                logDebug("One to One call")
                val call: MegaChatCall? = inMeetingViewModel.getCall()
                call?.let { call ->
                    val session = inMeetingViewModel.getSessionOneToOneCall(call)
                    session?.let { session ->
                        individualCallFragment = IndividualCallFragment.newInstance(
                            call.chatid,
                            session.peerid,
                            session.clientid
                        )

                        individualCallFragment?.let { it ->
                            loadChildFragment(
                                R.id.meeting_container,
                                it,
                                IndividualCallFragment.TAG
                            )
                        }
                    }

                    initLocal(call.chatid)
                }
            }
        }
    }

    /**
     * Show waitingForConnection UI
     */
    private fun waitingForConnection(chatId: Long) {
        when (status) {
            TYPE_WAITING_CONNECTION -> return
            else -> {
                logDebug("Waiting for connection call")
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
                }

                checkGridSpeakerViewMenuItemVisibility()
            }
        }
    }

    /**
     * Show local fragment UI
     */
    private fun initLocal(chatId: Long) {
        when (floatingWindowFragment) {
            null -> {
                floatingWindowFragment = IndividualCallFragment.newInstance(
                    chatId,
                    megaChatApi.myUserHandle,
                    true
                )
            }
        }

        floatingWindowFragment?.let {
            loadChildFragment(
                R.id.self_feed_floating_window_container,
                it,
                IndividualCallFragment.TAG
            )
        }
    }

    private fun initSpeakerViewMode() {
        when (status) {
            TYPE_IN_SPEAKER_VIEW -> {
                return
            }
            else -> {
                status = TYPE_IN_SPEAKER_VIEW

                logDebug("Group call - Speaker View")

                gridViewCallFragment?.let {
                    removeChildFragment(it)
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

                updateParticipantRes(
                    inMeetingViewModel.checkParticipantsResolution(status)
                )

                checkGridSpeakerViewMenuItemVisibility()
            }
        }
    }

    private fun initGridViewMode() {
        if (status == TYPE_IN_GRID_VIEW) {
            return
        }

        status = TYPE_IN_GRID_VIEW
        logDebug("Group call - Grid View")

        speakerViewCallFragment?.let {
            removeChildFragment(it)
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
        updateParticipantRes(
            inMeetingViewModel.checkParticipantsResolution(status)
        )
        checkGridSpeakerViewMenuItemVisibility()
    }

    /**
     * Show meeting UI
     */
    private fun initGroupCall(chatId: Long) {
        if (status != TYPE_IN_GRID_VIEW && status != TYPE_IN_SPEAKER_VIEW) {
            initLocal(chatId)
        }

        if (!isManualModeView) {
            inMeetingViewModel.getCall()?.let {
                if (it.numParticipants <= MAX_PARTICIPANTS_GRID_VIEW_AUTOMATIC) {
                    initGridViewMode()
                } else {
                    initSpeakerViewMode()
                }
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

                inMeetingViewModel.startMeeting(
                    micIsEnable,
                    camIsEnable,
                    StartChatCallListener(requireContext(), this, this)
                )
            }
        }
    }

    private fun loadChildFragment(containerId: Int, fragment: Fragment, tag: String) {
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
     */
    private fun updateToolbarSubtitle(call: MegaChatCall) {
        when (call.status) {
            MegaChatCall.CALL_STATUS_CONNECTING -> {
                CallUtil.activateChrono(false, meetingChrono, null)
                toolbarSubtitle?.let {
                    toolbarSubtitle?.text = StringResourcesUtils.getString(R.string.chat_connecting)
                }
            }
            MegaChatCall.CALL_STATUS_JOINING, MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                when {
                    inMeetingViewModel.isRequestSent() && !MegaApplication.isCreatingMeeting(
                        inMeetingViewModel.getChatId()
                    ) -> {
                        CallUtil.activateChrono(false, meetingChrono, null)
                        toolbarSubtitle?.let {
                            toolbarSubtitle?.text =
                                StringResourcesUtils.getString(R.string.outgoing_call_starting)
                        }

                    }
                    else -> {
                        toolbarSubtitle?.let {
                            toolbarSubtitle?.text =
                                StringResourcesUtils.getString(R.string.duration_meeting)
                        }
                        CallUtil.activateChrono(true, meetingChrono, call)
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

        checkGridSpeakerViewMenuItemVisibility()
        checkSwapCameraMenuItemVisibility()
    }

    private fun checkSwapCameraMenuItemVisibility() {
        swapCameraMenuItem?.let {
            it.isVisible = inMeetingViewModel.isNecessaryToShowSwapCameraOption()
        }
    }

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
        showSnackbar(Constants.PERMISSIONS_TYPE, warningText, MEGACHAT_INVALID_HANDLE)
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
                isGuest,
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
                    it.isVisible = true
                    if (type != TYPE_NETWORK_QUALITY) {
                        it.animate()?.alpha(0f)?.duration = INFO_ANIMATION.toLong()
                    }
                }
                else -> {
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
                bannerShouldBeShown && toolbar.isVisible -> {
                    it.background = ContextCompat.getDrawable(
                        context,
                        R.drawable.gradient_shape_callschat
                    )
                    it.isVisible = true
                }
                else -> {
                    it.isVisible = false
                }
            }

            when (BottomSheetBehavior.STATE_EXPANDED) {
                bottomFloatingPanelViewHolder.getState() -> {
                    when {
                        bannerInfoLayout.isVisible -> {
                            bannerInfoLayout.alpha = 0f
                        }
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
        bottomFloatingPanelViewHolder.let {
            it.enableHoldIcon(
                !inMeetingViewModel.isRequestSent(),
                callIsOnHold
            )
        }
    }

    /**
     * Method that controls whether the call has been put on or taken off hold, and updates the UI.
     */
    private fun isCallOnHold(isHold: Boolean) {
        bottomFloatingPanelViewHolder.let {
            bottomFloatingPanelViewHolder.updateHoldIcon(isHold)
        }

        showBannerInfo()
        when {
            !inMeetingViewModel.isOneToOneCall() -> {
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

    private fun updateLowOrHiResolution(isHiRes: Boolean, session: MegaChatSession) {
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateRemoteResolution(session)
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.updateRemoteResolution(isHiRes, session)
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
        gridViewCallFragment?.let {
            if (it.isAdded) {
                when {
                    isAudioChange -> {
                        it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                    }
                }
                when {
                    isVideoChange -> {
                        it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                    }
                }
            }
        }

        speakerViewCallFragment?.let {
            if (it.isAdded) {
                when {
                    isAudioChange -> {
                        it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                    }
                }
                when {
                    isVideoChange -> {

                        it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                    }
                }
            }
        }

        bottomFloatingPanelViewHolder.let {
            it.updateRemoteAudioVideo(session)
        }
    }

    private fun participantAddedOfLeftMeeting(isAdded: Boolean, position: Int) {
        speakerViewCallFragment?.let {
            if (it.isAdded) {
                it.peerAddedOrRemoved(isAdded, position, inMeetingViewModel.participants.value!!)
            }
        }

        updateParticipantRes(
            inMeetingViewModel.checkParticipantsResolution(status)
        )
    }

    /**
     * Method that checks if several participants resolution has changed and updates the UI
     */
    private fun updateParticipantRes(listParticipants: MutableSet<Participant>) {
        if (listParticipants.isNotEmpty()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateRes(listParticipants)
                }
            }
            speakerViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateRes(listParticipants)
                }
            }
        }
    }

    /**
     * Method that checks if a participant's name has changed and updates the UI
     */
    private fun updateParticipantName(peerId: Long) {
        var listParticipants = inMeetingViewModel.updateParticipantsName(peerId)
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
     */
    private fun updateRemotePrivileges(listParticipants: MutableSet<Participant>) {
        if (listParticipants.isNotEmpty()) {
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
                inMeetingViewModel.leaveMeeting()
                checkIfAnotherCallShouldBeShown()
            }
            isModerator && inMeetingViewModel.shouldAssignModerator() -> {
                val endMeetingBottomSheetDialogFragment =
                    EndMeetingBottomSheetDialogFragment.newInstance(inMeetingViewModel.getChatId())
                endMeetingBottomSheetDialogFragment.show(
                    parentFragmentManager,
                    endMeetingBottomSheetDialogFragment.tag
                )
            }
            else ->
                askConfirmationEndMeetingForUser()
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
            isGuest -> {
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
            }
        }
    }

    /**
     * Method to control when call ended
     */
    private fun finishActivity() {
        MegaApplication.setCreatingMeeting(inMeetingViewModel.getChatId(), false)
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
            sharedModel.createChatLink(inMeetingViewModel.getChatId())
            logError("Error, the link doesn't exist")
            return
        }

        shareLink()
    }

    fun shareLink() {
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
                putExtra("contactType", Constants.CONTACT_TYPE_MEGA)
                putExtra("chat", true)
                putExtra("chatId", 123L)
                putExtra("aBtitle", getString(R.string.invite_participants))
            }
        meetingActivity.startActivityForResult(
            inviteParticipantIntent, Constants.REQUEST_ADD_PARTICIPANTS
        )
    }

    /**
     * Show participant bottom sheet when user click the three dots on participant item
     */
    override fun onParticipantOption(participant: Participant) {
        val participantBottomSheet =
            MeetingParticipantBottomSheetDialogFragment.newInstance(
                isGuest,
                isModerator,
                status == TYPE_IN_SPEAKER_VIEW,
                participant
            )
        participantBottomSheet.show(parentFragmentManager, participantBottomSheet.tag)
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
    }

    private fun checkCallStarted(chatId: Long) {
        MegaApplication.getInstance().openCallService(chatId)
        inMeetingViewModel.setCall(chatId)
        checkChildFragments()
        showBannerInfo()
        checkAnotherCall()
    }

    override fun onCallStarted(chatId: Long, enableVideo: Boolean, enableAudio: Int) {
        MegaApplication.setCreatingMeeting(chatId, true)
        checkCallStarted(chatId)
    }

    override fun onDestroy() {
        super.onDestroy()

        CallUtil.activateChrono(false, meetingChrono, null)
        resumeAudioPlayerIfNotInCall(meetingActivity)
    }

    override fun onCallAnswered(chatId: Long, flag: Boolean) {
        if (chatId == inMeetingViewModel.getChatId()) {
            MegaApplication.setOpeningMeetingLink(args.chatId, false)
            checkCallStarted(chatId)
        }
    }

    override fun onErrorAnsweredCall(errorCode: Int) {
        logDebug("Error answering the meeting so close it")
        MegaApplication.setOpeningMeetingLink(args.chatId, false)
        finishActivity()
    }

    override fun onJoinedChat(chatId: Long, userHandle: Long) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            sharedModel.updateChatRoomId(chatId)
            inMeetingViewModel.setChatId(chatId)
        }

        inMeetingViewModel.getCall()?.let {
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


    fun updatePanelParticipantList(list: MutableList<Participant> = mutableListOf()) {
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
            finishActivity()
        } else {
            CallUtil.openMeetingInProgress(requireContext(), anotherCall.chatid, false)
            finishActivity()
        }
    }
}