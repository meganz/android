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
import mega.privacy.android.app.MegaApplication.setWasLocalVideoEnable
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
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
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom.CHANGE_TYPE_OWN_PRIV

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    StartChatCallListener.OnCallStartedCallback {

    val args: InMeetingFragmentArgs by navArgs()

    // Views
    lateinit var toolbar: MaterialToolbar

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null

    private lateinit var bannerInfoLayout: View
    private var bannerText: TextView? = null
    private var bannerIcon: ImageView? = null

    private var bannerParticipant: TextView? = null

    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private var swapCameraMenuItem: MenuItem? = null
    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem

    private var micIsEnable = false
    private var camIsEnable = false
    private var meetinglink: String = ""
    private var inTemporaryState = false

    // Children fragments
    private var individualCallFragment: IndividualCallFragment? = null
    private var floatingWindowFragment: IndividualCallFragment? = null
    private var gridViewCallFragment: GridViewCallFragment? = null
    //private var speakerViewCallFragment: SpeakerViewCallFragment? = null

    // Flags, should get the value from somewhere
    private var isGuest = false
    private var isModerator = true

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

    private val privilegesChangeObserver = Observer<MegaChatListItem> {
        when {
            inMeetingViewModel.isSameChatRoom(it.chatId) -> {
                inMeetingViewModel.getCall()?.let {
                    when (it.status) {
                        MegaChatCall.CALL_STATUS_IN_PROGRESS -> {
                            when {
                                it.hasChanged(CHANGE_TYPE_OWN_PRIV) && inMeetingViewModel.getOwnPrivileges() == MegaChatRoom.PRIV_MODERATOR -> {
                                    showFixedBanner(megaChatApi.myUserHandle, TYPE_OWN_PRIVILEGE)
                                }
                                it.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS) -> {
                                    updateRemotePrivileges(inMeetingViewModel.updateParticipantsPrivileges())
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
            inMeetingViewModel.isSameCall(it.callId) && it.status != INVALID_CALL_STATUS -> {
                updateToolbarSubtitle(it)
                when (it.status) {
                    MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED -> finishActivity()
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
                                        setWasLocalVideoEnable(true)
                                        sharedModel.clickCamera(false)
                                    }
                                    else -> {
                                        setWasLocalVideoEnable(false)
                                    }
                                }
                            }
                            else -> {
                                updateOnHoldRemote(callAndSession.second)
                            }
                        }
                    }
                }
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            when {
                inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall() -> {
                    when (callAndSession.second.status) {
                        MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                            if (inMeetingViewModel.createParticipant(callAndSession.second)) {
                                updateParticipantRes(
                                    inMeetingViewModel.checkParticipantsResolution(
                                        true
                                    )
                                )
                            }
                        }
                        MegaChatSession.SESSION_STATUS_DESTROYED -> {
                            if (inMeetingViewModel.removeParticipant(callAndSession.second)) {
                                updateParticipantRes(
                                    inMeetingViewModel.checkParticipantsResolution(
                                        false
                                    )
                                )
                            }
                        }
                    }
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
                sharedModel.updateChatRoomId(it)
                inMeetingViewModel.setChatId(it)
            }
        }

        val meetingName: String? =
            arguments?.getString(MeetingActivity.MEETING_NAME)

        meetingName?.let {
            if (!TextUtil.isTextEmpty(it)) {
                sharedModel.setMeetingsName(it)
            }
        }

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
    }

    private fun takeActionByArgs() {
        when (args.action) {
            MEETING_ACTION_CREATE -> initStartMeeting()
            MEETING_ACTION_JOIN -> {
                inMeetingViewModel.joinPublicChat(args.chatId)
            }
            MEETING_ACTION_GUEST -> {
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
    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbarTitle = meetingActivity.title_toolbar
        toolbarSubtitle = meetingActivity.subtitle_toolbar
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
            if (!it.isNullOrEmpty()) {
                meetinglink = it
                if (inMeetingViewModel.isWaitingForLink()) {
                    inMeetingViewModel.setWaitingForLink(false)
                    shareLink()
                } else {
                    inMeetingViewModel.startMeeting(
                        micIsEnable,
                        camIsEnable,
                        StartChatCallListener(meetingActivity, this, this)
                    )
                }
            }
        }

        sharedModel.meetingNameLiveData.observe(viewLifecycleOwner) {
            if (!TextUtil.isTextEmpty(it)) {
                inMeetingViewModel.setTitleChat(it)
            }
        }

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            if (toolbarTitle != null) {
                toolbarTitle?.text = it
            }
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            if (micIsEnable != it) {
                micIsEnable = it
                updateLocalAudio(it)
            }
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            if (camIsEnable != it) {
                camIsEnable = it
                updateLocalVideo(it)
            }
        }

        sharedModel.speakerLiveData.observe(viewLifecycleOwner) {
            updateSpeaker(it)
        }

        /**
         * Will Change after Andy modify the permission structure
         */
        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                checkMeetingPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                ) { showRequestPermissionSnackBar() }
            }
        }
        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                checkMeetingPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                ) { showRequestPermissionSnackBar() }
            }
        }

        inMeetingViewModel.joinPublicChat.observe(viewLifecycleOwner) {
            inMeetingViewModel.answerChatCall(camIsEnable, micIsEnable)
        }
    }

    private fun initFloatingWindowContainerDragListener(view: View) {
        dragTouchListener = OnDragTouchListener(
            floatingWindowContainer,
            view,
            object : OnDragTouchListener.OnDragActionListener {

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
                inMeetingViewModel.isOnlyMeOnTheCall(it.chatid) -> {
                    waitingForConnection(it.chatid)
                }
                else -> {
                    val session = CallUtil.getSessionIndividualCall(it)
                    session?.let {
                        logDebug("Session exists")
                        initOneToOneCall()
                    }
                }
            }
        }
    }

    /**
     * Show the correct UI in a meeting
     */
    private fun updateGroupUI() {
        inMeetingViewModel.getCall()?.let { call ->
            when {
                inMeetingViewModel.isOnlyMeOnTheCall(call.chatid) -> {
                    waitingForConnection(call.chatid)
                }
                else -> {
                    val session = CallUtil.getSessionIndividualCall(call)
                    session?.let {
                        logDebug("Session exists")
                        initGroupCall(call.chatid)
                    }
                }
            }
        }
    }

    /**
     * Control the UI of the call, whether one-to-one or meeting
     */
    private fun checkChildFragments() {
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
                logDebug("One to One call and in progress call")
                val call: MegaChatCall? = inMeetingViewModel.getCall()
                call?.let { call ->
                    val session = CallUtil.getSessionIndividualCall(call)
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
                logDebug("One to One call and outgoing call")
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
            }
        }
    }

    /**
     * Show local fragment UI
     */
    private fun initLocal(chatId: Long) {
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

    private fun initSpeakerViewMode() {
        if (status == TYPE_IN_SPEAKER_VIEW) {
            return
        }

        status = TYPE_IN_SPEAKER_VIEW
        logDebug("Group call - Speaker View")

//        if(speakerViewCallFragment == null){
//            speakerViewCallFragment = SpeakerViewCallFragment().newInstance()
//        }
//
//        speakerViewCallFragment?.let {
//            loadChildFragment(
//                R.id.meeting_container,
//                it,
//                SpeakerViewCallFragment.TAG
//            )
//        }
    }

    private fun initGridViewMode() {
        if (status == TYPE_IN_GRID_VIEW) {
            return
        }
        status = TYPE_IN_GRID_VIEW
        logDebug("Group call - Grid View")

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
    }

    /**
     * Show meeting UI
     */
    private fun initGroupCall(chatId: Long) {
        if (status != TYPE_IN_GRID_VIEW && status != TYPE_IN_SPEAKER_VIEW) {
            initLocal(chatId)
        }

        inMeetingViewModel.getCall()?.let {
            if (it.numParticipants > 6) {
                initSpeakerViewMode()
            } else {
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
                    inMeetingViewModel.isRequestSent() -> {
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

        if (inMeetingViewModel.isOneToOneCall() || inMeetingViewModel.isRequestSent()) {
            gridViewMenuItem.isVisible = false
            speakerViewMenuItem.isVisible = false
        } else {
            speakerViewMenuItem.isVisible = false
            gridViewMenuItem.isVisible = true
        }

        checkSwapCameraMenuItemVisibility()
    }

    private fun checkSwapCameraMenuItemVisibility() {
        swapCameraMenuItem?.isVisible = inMeetingViewModel.isNecessaryToShowSwapCameraOption()
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
                gridViewMenuItem.isVisible = false
                speakerViewMenuItem.isVisible = true
                initGridViewMode()
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
//                inMeetingViewModel.setSpeakerViewManual()
//                gridViewMenuItem.isVisible = true
//                speakerViewMenuItem.isVisible = false
//                initSpeakerViewMode()
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
                binding,
                this,
                isGuest,
                isModerator,
                !inMeetingViewModel.isOneToOneCall()
            )

        /**
         * Observer the participant List
         */
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants.let {
                bottomFloatingPanelViewHolder.setParticipants(it)
            }
        }

        bottomFloatingPanelViewHolder.propertyUpdaters.add {
            toolbar.alpha = 1 - it
            when {
                bannerInfoLayout.isVisible -> {
                    bannerInfoLayout.alpha = 1 - it
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
            if (inMeetingViewModel.showBannerFixedBanner(it, peerId, type) &&
                bottomFloatingPanelViewHolder.getState() != BottomSheetBehavior.STATE_EXPANDED
            ) {
                it.alpha = 1f
                it.isVisible = true
                if (type != TYPE_NETWORK_QUALITY) {
                    it.animate()?.alpha(0f)?.duration = INFO_ANIMATION.toLong()
                }
            } else {
                it.isVisible = false
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
        if (!inMeetingViewModel.isOneToOneCall()) {
            gridViewCallFragment?.let {
                if (it.isAdded) {
                    it.updateCallOnHold(isHold)
                }
            }
//            speakerViewCallFragment?.let {
//                if (it.isAdded) {
//                    it.updateCallOnHold(isHold)
//                }
//            }
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
                if (camOn) {
                    it.activateVideo(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.showAvatar(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                }
            }
        }

        floatingWindowFragment?.let {
            if (it.isAdded) {
                if (camOn) {
                    it.activateVideo(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                } else {
                    it.showAvatar(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
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
            if (!inTemporaryState) {
                MegaApplication.setVideoStatus(it.chatid, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(camOn)
        checkSwapCameraMenuItemVisibility()
        controlVideoLocalOneToOneCall(camOn)
    }

    /**
     * Method that checks if the session's on hold state has changed and updates the UI
     */
    private fun updateOnHoldRemote(session: MegaChatSession) {
        gridViewCallFragment?.let {
            if (it.isAdded) {
                it.updateSessionOnHold(session)
            }
        }
//        speakerViewCallFragment?.let {
//            if (it.isAdded) {
//                it.updateSessionOnHold(session)
//            }
//        }
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
                if (isAudioChange) {
                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
                }
                if (isVideoChange) {
                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
                }
            }
        }

//        speakerViewCallFragment?.let {
//            if (it.isAdded) {
//                if (isAudioChange) {
//                    it.updateRemoteAudioVideo(TYPE_AUDIO, session)
//                }
//                if (isVideoChange) {
//                    it.updateRemoteAudioVideo(TYPE_VIDEO, session)
//                }
//            }
//        }
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
//            speakerViewCallFragment?.let {
//                if (it.isAdded) {
//                    it.updateRes(listParticipants)
//                }
//            }
        }
    }

    /**
     * Method that checks if a participant's name has changed and updates the UI
     */
    private fun updateParticipantName(peerId: Long) {
        var listParticipants = inMeetingViewModel.existsParticipants(peerId)
        if (listParticipants.isNotEmpty()) {
            gridViewCallFragment?.let {
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
//            speakerViewCallFragment?.let {
//                if (it.isAdded) {
//                    it.updatePrivileges(listParticipants)
//                }
//            }
        }
    }

    /**
     * Change Hold State
     */
    override fun onChangeHoldState(isHold: Boolean) {
        inMeetingViewModel.setCallOnHold(isHold)
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
                finishActivity()
            }
            isModerator && inMeetingViewModel.haveOneModerator() -> {
                val endMeetingBottomSheetDialogFragment =
                    EndMeetingBottomSheetDialogFragment.newInstance()
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
                !speakerViewMenuItem.isVisible,
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

    override fun onCallStarted(chatId: Long, enableVideo: Boolean, enableAudio: Int) {
        MegaApplication.getInstance().openCallService(chatId)
        inMeetingViewModel.setCall(chatId)
        checkChildFragments()
        showBannerInfo()
    }

    override fun onDestroy() {
        super.onDestroy()

        CallUtil.activateChrono(false, meetingChrono, null)
        inMeetingViewModel.isSpeakerViewAutomatic()

        resumeAudioPlayerIfNotInCall(meetingActivity)
    }
}