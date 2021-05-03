package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.appbar.MaterialToolbar
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
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.pauseAudioPlayer
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService.Companion.resumeAudioPlayerIfNotInCall
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.INVALID_CALL_STATUS
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatSession

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    StartChatCallListener.OnCallStartedCallback {

    // Views
    lateinit var toolbar: MaterialToolbar

    private var toolbarTitle: EmojiTextView? = null
    private var toolbarSubtitle: TextView? = null
    private var meetingChrono: Chronometer? = null

    private var bannerInfoLayout: LinearLayout? = null
    private var bannerText: TextView? = null
    private var bannerIcon: ImageView? = null

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
    private lateinit var gridViewCallFragment: GridViewCallFragment
    private lateinit var speakerViewCallFragment: SpeakerViewCallFragment

    // Flags, should get the value from somewhere
    private var isGuest = false
    private var isModerator = true

    private var status = NOT_TYPE

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener

    private lateinit var binding: InMeetingFragmentBinding

    val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private val proximitySensorChangeObserver = Observer<Boolean> {
        val chatId = inMeetingViewModel.getChatId()
        if (chatId != MEGACHAT_INVALID_HANDLE && inMeetingViewModel.getCall() != null) {
            val realStatus = MegaApplication.getVideoStatus(chatId)
            if (!realStatus) {
                inTemporaryState = false
            } else if (it) {
                inTemporaryState = true
                sharedModel.clickCamera(false)
            } else {
                inTemporaryState = false
                sharedModel.clickCamera(true)
            }
        }
    }

    private val errorStatingCallObserver = Observer<Long> {
        if (inMeetingViewModel.isSameChatRoom(it)) {
            MegaApplication.getInstance().removeRTCAudioManager()
            meetingActivity.finish()
        }
    }

    private val noOutgoingCallObserver = Observer<Long> { callId ->
        if (inMeetingViewModel.isSameCall(callId)) {
            val call = inMeetingViewModel.getCall()
            call?.let { chatCall ->
                updateToolbarSubtitle(chatCall)
                enableOnHoldFab(chatCall.isOnHold)
            }
        }
    }

    private val callStatusObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) && it.status != INVALID_CALL_STATUS) {
            updateToolbarSubtitle(it)
            when (it.status) {
                MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION, MegaChatCall.CALL_STATUS_DESTROYED -> finishActivity()
            }
        }
    }

    private val callCompositionObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId) && it.status != INVALID_CALL_STATUS && (it.callCompositionChange == 1 || it.callCompositionChange == -1)) {
            if (inMeetingViewModel.isOneToOneCall()) {
                if (it.numParticipants == 1 || it.numParticipants == 2) {
                    updateChildFragments()
                }
            } else {
                updateChildFragments()
                userJoinOrLeaveTheMeeting(it.peeridCallCompositionChange, it.callCompositionChange)
            }
        }
    }

    private val callOnHoldObserver = Observer<MegaChatCall> {
        if (inMeetingViewModel.isSameCall(it.callId)) {
            isCallOnHold(it.isOnHold)
        }
    }

    private val sessionOnHoldObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                updateBannerInfo()
                val call = inMeetingViewModel.getCall()
                call?.let {
                    if (it.hasLocalVideo() && callAndSession.second.isOnHold) {
                        setWasLocalVideoEnable(true)
                        sharedModel.clickCamera(false)
                    } else {
                        setWasLocalVideoEnable(false)
                    }
                }
            }
        }

    private val sessionStatusObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first) && !inMeetingViewModel.isOneToOneCall()) {
                when (callAndSession.second.status) {
                    MegaChatSession.SESSION_STATUS_IN_PROGRESS -> {
                        inMeetingViewModel.createParticipant(callAndSession.second)

                    }
                    MegaChatSession.SESSION_STATUS_DESTROYED -> {

                    }
                }

            }
        }

    private val remoteAVFlagsObserver =
        Observer<Pair<Long, MegaChatSession>> { callAndSession ->
            //As the session has been established, I am no longer in the Request sent state
            if (inMeetingViewModel.isSameCall(callAndSession.first)) {
                updateBannerInfo()
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
        initFloatingPanel()

        val chatId: Long? =
            arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)

        chatId?.let {
            if (it != MEGACHAT_INVALID_HANDLE) {
                sharedModel.updateChatRoom(it)
                inMeetingViewModel.setChat(it)
            }
        }

        val meetingName: String? =
            arguments?.getString(MeetingActivity.MEETING_NAME)

        meetingName?.let {
            if (!TextUtil.isTextEmpty(meetingName)) {
                sharedModel.setMeetingsName(meetingName)
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
                    currentCall.hasLocalAudio() -> {
                        sharedModel.micInitiallyOn()
                    }
                }
                when {
                    currentCall.hasLocalVideo() -> {
                        sharedModel.camInitiallyOn()
                    }
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
        initFloatingWindowContainerDragListener(view)
        initChildFragments()
        initStartMeeting()
        // Set on page tapping listener.
        setPageOnClickListener(view)
        setSystemUIVisibility()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set parent activity can receive the orientation changes
        meetingActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        pauseAudioPlayer(meetingActivity)

    }

    /**
     * Observe the Orientation changes
     *
     * @param newConfig
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLayout(newConfig.orientation)
    }

    /**
     * Update the layout for landscape and portrait screen
     *
     * @param orientation the flag of the orientation
     */
    private fun updateLayout(orientation: Int) {
        // Add the Changes for the in-meeting-fragment
        bottomFloatingPanelViewHolder.updateWidth(orientation)
    }

    private fun initLiveEventBus() {
        LiveEventBus.get(Constants.EVENT_PROXIMITY_SENSOR_CHANGE, Boolean::class.java)
            .observeForever(proximitySensorChangeObserver)

        LiveEventBus.get(Constants.EVENT_ERROR_STARTING_CALL, Long::class.java)
            .observeForever(errorStatingCallObserver)

        LiveEventBus.get(Constants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observeForever(noOutgoingCallObserver)

        LiveEventBus.get(Constants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callStatusObserver)

        LiveEventBus.get(Constants.EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .observeForever(callCompositionObserver)

        LiveEventBus.get(Constants.EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .observeForever(callOnHoldObserver)

        LiveEventBus.get(Constants.EVENT_SESSION_STATUS_CHANGE)
            .observeForever(sessionStatusObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_SESSION_ON_HOLD_CHANGE)
            .observeForever(sessionOnHoldObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_REMOTE_AVFLAGS_CHANGE)
            .observeForever(remoteAVFlagsObserver as Observer<Any>)

    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbarTitle = meetingActivity.title_toolbar
        toolbarSubtitle = meetingActivity.subtitle_toolbar
        bannerInfoLayout = meetingActivity.banner_info_layout
        bannerIcon = meetingActivity.banner_icon
        bannerText = meetingActivity.banner_info
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
        sharedModel.chatRoomLiveData.observe(viewLifecycleOwner) {
            it?.let {
                inMeetingViewModel.setChat(it.chatId)
            }
        }

        sharedModel.meetingLinkLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                meetinglink = it
                if (inMeetingViewModel.isWaitingForLink()) {
                    inMeetingViewModel.setWaitingForLink(true)
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

        inMeetingViewModel.callLiveData.observe(viewLifecycleOwner) {
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
    }

    private fun initFloatingWindowContainerDragListener(view: View) {
        dragTouchListener = OnDragTouchListener(
            floatingWindowContainer,
            view,
            object : OnDragTouchListener.OnDragActionListener {

                override fun onDragStart(view: View?) {
                    if (toolbar.isVisible) {
                        dragTouchListener.setToolbarHeight(toolbar.bottom)
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

    private fun setSystemUIVisibility() {
        // Set system UI color to make them visible.
        // decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        meetingActivity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 0x00000010
    }

    private fun setPageOnClickListener(view: View) = view.setOnClickListener {
        onPageClick()
    }

    fun onPageClick() {
        // Prevent fast tapping.
        if (System.currentTimeMillis() - lastTouch < TAP_THRESHOLD) return

        toolbar.fadeInOut(dy = TOOLBAR_DY, toTop = true)
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
        val isIntersect = (toolbar.bottom - floatingWindowContainer.y) > 0
        if (toolbar.isVisible && isIntersect) {
            floatingWindowContainer.moveY(toolbar.bottom.toFloat())
        }

        val isIntersectPreviously = (toolbar.bottom - previousY) > 0
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
     * Show the correct UI in One-to-one call
     */
    private fun updateOneToOneUI() {
        inMeetingViewModel.getCall()?.let {
            if (inMeetingViewModel.isOnlyMeOnTheCall(it.chatid)) {
                waitingForConnection(it.chatid)
            } else {
                val session = CallUtil.getSessionIndividualCall(it)
                session?.let {
                    logDebug("Session exists")
                    initOneToOneCall()
                }
            }
        }
    }

    private fun updateGroupUI() {
        inMeetingViewModel.getCall()?.let { call ->
            if (inMeetingViewModel.isOnlyMeOnTheCall(call.chatid)) {
                waitingForConnection(call.chatid)
            } else {
                val session = CallUtil.getSessionIndividualCall(call)
                session?.let {
                    logDebug("Session exists")
                    initGroupCall(call.chatid)
                }
            }
        }
    }

    private fun initChildFragments() {
        val chatId = inMeetingViewModel.getChatId()
        if (chatId == MEGACHAT_INVALID_HANDLE)
            return

        updateChildFragments()
    }

    private fun updateChildFragments() {
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

    private fun initOneToOneCall() {
        if (status == TYPE_IN_ONE_TO_ONE)
            return

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

    private fun waitingForConnection(chatId: Long) {
        if (status == TYPE_WAITING_CONNECTION)
            return

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

    private fun initLocal(chatId: Long) {
        floatingWindowFragment = IndividualCallFragment.newInstance(
            chatId,
            megaChatApi.myUserHandle,
            true
        )
        floatingWindowFragment?.let {
            loadChildFragment(
                R.id.self_feed_floating_window_container,
                it,
                IndividualCallFragment.TAG
            )
        }
    }

    private fun initGroupCall(chatId: Long) {
        if (status == TYPE_IN_GRID_VIEW)
            return

        logDebug("Group call")
        status = TYPE_IN_GRID_VIEW

        //initLocal(chatId)
        gridViewCallFragment = GridViewCallFragment.newInstance()
        loadChildFragment(
            R.id.meeting_container,
            gridViewCallFragment,
            GridViewCallFragment.TAG
        )

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
    }

    private fun initStartMeeting() {
        if (sharedModel.chatRoomLiveData.value == null) {
            logDebug("Starting call")
            val nameChosen: String? = sharedModel.getMeetingName()
            nameChosen?.let {
                if (!TextUtil.isTextEmpty(it)) {
                    inMeetingViewModel.setTitleChat(it)
                }
            }
            inMeetingViewModel.startMeeting(
                micIsEnable,
                camIsEnable,
                StartChatCallListener(requireContext(), this, this)
            )
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

                loadChildFragment(
                    R.id.meeting_container,
                    gridViewCallFragment,
                    GridViewCallFragment.TAG
                )
                true
            }
            R.id.speaker_view -> {
                logDebug("Change to speaker view.")
                inMeetingViewModel.setSpeakerViewManual()
                gridViewMenuItem.isVisible = true
                speakerViewMenuItem.isVisible = false

                loadChildFragment(
                    R.id.meeting_container,
                    speakerViewCallFragment,
                    SpeakerViewCallFragment.TAG
                )
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
                inMeetingViewModel.isOneToOneCall()
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
        updateBannerInfo()
    }

    private fun userJoinOrLeaveTheMeeting(peerId: Long, type: Int) {
        bannerInfoLayout?.let {
            var shouldBeShown =
                inMeetingViewModel.showBannerUserJoinOrLeaveCall(bannerText, peerId, type)
            if (shouldBeShown == true) {
                bannerInfoLayout?.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.teal_300
                    )
                )
                bannerInfoLayout?.alpha = 1f
                bannerInfoLayout?.isVisible = true
                bannerInfoLayout?.animate()?.alpha(0f)?.setDuration(INFO_ANIMATION.toLong())
            } else {
                bannerInfoLayout?.isVisible = false
            }
        }
    }

    private fun updateBannerInfo() {
        if (!inMeetingViewModel.isOneToOneCall()) {
            return
        }

        bannerInfoLayout?.let {
            bannerInfoLayout?.isVisible =
                inMeetingViewModel.showAppropriateBannerOneToOneCall(bannerIcon, bannerText)
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

    private fun isCallOnHold(isHold: Boolean) {
        bottomFloatingPanelViewHolder.updateHoldIcon(isHold)
        updateBannerInfo()
    }

    /**
     * Change Cam State
     */
    override fun onChangeCamState(camOn: Boolean) {
        sharedModel.clickCamera(!camOn)
    }

    private fun controlVideoLocalOneToOneCall(camOn: Boolean) {
        if (inMeetingViewModel.isRequestSent()) {
            individualCallFragment?.let {
                if (it.isAdded) {
                    if (camOn) {
                        it.activateVideo(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    } else {
                        it.showAvatar(megaChatApi.myUserHandle, MEGACHAT_INVALID_HANDLE)
                    }
                }
            }
        } else {
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
    }

    private fun updateLocalVideo(camOn: Boolean) {
        inMeetingViewModel.getCall()?.let {
            val isVideoOn: Boolean = it.hasLocalVideo()
            if (!inTemporaryState) {
                MegaApplication.setVideoStatus(it.chatid, isVideoOn)
            }
        }

        bottomFloatingPanelViewHolder.updateCamIcon(camOn)
        swapCameraMenuItem?.isVisible = inMeetingViewModel.isNecessaryToShowSwapCameraOption()
        controlVideoLocalOneToOneCall(camOn)
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
            isModerator && inMeetingViewModel.haveOneModerator()  -> {
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
        inMeetingViewModel.setCall(chatId)
        initChildFragments()
    }

    override fun onDestroy() {
        super.onDestroy()

        LiveEventBus.get(Constants.EVENT_PROXIMITY_SENSOR_CHANGE, Boolean::class.java)
            .removeObserver(proximitySensorChangeObserver)

        LiveEventBus.get(Constants.EVENT_ERROR_STARTING_CALL, Long::class.java)
            .removeObserver(errorStatingCallObserver)

        LiveEventBus.get(Constants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .removeObserver(noOutgoingCallObserver)

        LiveEventBus.get(Constants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .removeObserver(callStatusObserver)

        LiveEventBus.get(Constants.EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall::class.java)
            .removeObserver(callCompositionObserver)

        LiveEventBus.get(Constants.EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall::class.java)
            .removeObserver(callOnHoldObserver)

        LiveEventBus.get(Constants.EVENT_SESSION_STATUS_CHANGE)
            .removeObserver(sessionStatusObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_SESSION_ON_HOLD_CHANGE)
            .removeObserver(sessionOnHoldObserver as Observer<Any>)

        LiveEventBus.get(Constants.EVENT_REMOTE_AVFLAGS_CHANGE)
            .removeObserver(remoteAVFlagsObserver as Observer<Any>)

        CallUtil.activateChrono(false, meetingChrono, null)
        inMeetingViewModel.isSpeakerViewAutomatic()

        resumeAudioPlayerIfNotInCall(meetingActivity)
    }
}