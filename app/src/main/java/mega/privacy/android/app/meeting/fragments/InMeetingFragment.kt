package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
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
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.InMeetingFragmentBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.lollipop.megachat.calls.OnDragTouchListener
import mega.privacy.android.app.meeting.AnimationTool.fadeInOut
import mega.privacy.android.app.meeting.AnimationTool.moveY
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.meeting.BottomFloatingPanelViewHolder
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall

@AndroidEntryPoint
class InMeetingFragment : MeetingBaseFragment(), BottomFloatingPanelListener, SnackbarShower,
    StartChatCallListener.OnCallStartedCallback {

    // Views
    lateinit var toolbar: MaterialToolbar
    private lateinit var floatingWindowContainer: View
    private lateinit var floatingBottomSheet: View

    lateinit var bottomFloatingPanelViewHolder: BottomFloatingPanelViewHolder

    private lateinit var gridViewMenuItem: MenuItem
    private lateinit var speakerViewMenuItem: MenuItem
    private lateinit var swapCameraMenuItem: MenuItem

    // Children fragments
    private lateinit var individualCallFragment: IndividualCallFragment
    private lateinit var floatingWindowFragment: IndividualCallFragment
    private lateinit var gridViewCallFragment: GridViewCallFragment
    private lateinit var speakerViewCallFragment: SpeakerViewCallFragment

    // Flags, should get the value from somewhere
    private var isGuest = false
    private var isModerator = true

    // For internal UI/UX use
    private var previousY = -1f
    private var lastTouch: Long = 0
    private lateinit var dragTouchListener: OnDragTouchListener

    private lateinit var binding: InMeetingFragmentBinding

    val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private val errorStatingCallObserver = Observer<Long> {
        if (sharedModel.chatRoomLiveData.value?.chatId == it) {
            MegaApplication.getInstance().removeRTCAudioManager()
            meetingActivity.finish()
        }
    }
    private val callStatusObserver = Observer<MegaChatCall> {
        if (sharedModel.callLiveData.value?.callId == it.callId) {
           enableOnHoldFab()
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

//        // TODO test code start: add x participants
//        val x = 2
//        for (i in 0 until x) {
//            inMeetingViewModel.addParticipant(true)
//        }
//        //TODO test code end

        LiveEventBus.get(Constants.EVENT_ERROR_STARTING_CALL, Long::class.java)
            .observeForever(errorStatingCallObserver)

        LiveEventBus.get(Constants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeForever(callStatusObserver)

        val chatId: Long? = arguments?.getLong(MeetingActivity.MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)
        sharedModel.updateChatAndCall(chatId!!)

        initCall()
        initToolbar()
        initShareViewModel()
        initFloatingWindowContainerDragListener(view)
        initChildrenFragments()
        initFloatingPanel()
        // Set on page tapping listener.
        setPageOnClickListener(view)
        setSystemUIVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()

        LiveEventBus.get(Constants.EVENT_ERROR_STARTING_CALL, Long::class.java)
            .removeObserver(errorStatingCallObserver)
        LiveEventBus.get(Constants.EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .removeObserver(callStatusObserver)
    }

    override fun onCallStarted(chatId: Long) {
        sharedModel.updateChatAndCall(chatId)
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

    private fun initChildrenFragments() {
        val chatId = sharedModel.chatRoomLiveData.value?.chatId ?: return

        val isRequestSent = sharedModel.isRequestSent()
        val isOneToOneChat = sharedModel.isOneToOneCall()

        if (isRequestSent) {
            waitingForConnection(chatId)
        } else {
            if (isOneToOneChat) {
                initOneToOneCall(chatId)
            } else {
                initGroupCall(chatId)
            }
        }
    }

    private fun waitingForConnection(chatId: Long) {
        //I am calling
        individualCallFragment = IndividualCallFragment.newInstance(
            chatId,
            megaChatApi.myUserHandle,
            false
        )

        loadChildFragment(
            R.id.meeting_container,
            individualCallFragment,
            IndividualCallFragment.TAG
        )
    }

    private fun initOneToOneCall(chatId: Long) {
        initLocal(chatId)

        val chatCall = sharedModel.callLiveData.value
        val clientId = chatCall?.sessionsClientid?.get(0)

        clientId?.let {
            val session = chatCall.getMegaChatSession(it)

            individualCallFragment = IndividualCallFragment.newInstance(
                chatId,
                session.peerid,
                session.clientid
            )

            loadChildFragment(
                R.id.meeting_container,
                individualCallFragment,
                IndividualCallFragment.TAG
            )
        }
    }

    private fun initLocal(chatId: Long) {
        //I am in a call in progress in a one to one chat
        floatingWindowFragment = IndividualCallFragment.newInstance(
            chatId,
            megaChatApi.myUserHandle,
            true
        )

        loadChildFragment(
            R.id.self_feed_floating_window_container,
            floatingWindowFragment,
            IndividualCallFragment.TAG
        )
    }

    private fun initGroupCall(chatId: Long) {
        initLocal(chatId)

        gridViewCallFragment = GridViewCallFragment.newInstance()
        loadChildFragment(
            R.id.meeting_container,
            gridViewCallFragment,
            GridViewCallFragment.TAG
        )

        speakerViewCallFragment = SpeakerViewCallFragment.newInstance()
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

    private fun initCall() {
        if (sharedModel.callLiveData.value == null) {
            sharedModel.startMeeting(StartChatCallListener(meetingActivity, this, this))
        }
    }

    private fun initToolbar() {
        toolbar = meetingActivity.toolbar
        toolbar.title = sharedModel.meetingNameLiveData.value
        updateToolbarSubtitle()

        meetingActivity.setSupportActionBar(toolbar)
        val actionBar = meetingActivity.supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        setHasOptionsMenu(true)
    }

    private fun updateToolbarSubtitle() {
        when {
            sharedModel.isRequestSent() -> {
                toolbar.subtitle = StringResourcesUtils.getString(R.string.outgoing_call_starting)
            }
            else -> {
                toolbar.subtitle = "Duration 00:00"
            }
        }
    }

    private fun checkIfSpeakerViewIsAutomatic(){
        if(MegaApplication.isSpeakerViewAutomatic(sharedModel.callLiveData.value!!.callId)){
            //Check to speakerView
        }else{
            //Keep grid view
        }

    }

    private fun loadChildFragment(containerId: Int, fragment: Fragment, tag: String) {
        childFragmentManager.beginTransaction().replace(
            containerId,
            fragment,
            tag
        ).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.in_meeting_fragment_menu, menu)

        speakerViewMenuItem = menu.findItem(R.id.speaker_view)
        gridViewMenuItem = menu.findItem(R.id.grid_view)
        swapCameraMenuItem = menu.findItem(R.id.swap_camera)

        if (sharedModel.isOneToOneCall()) {
            gridViewMenuItem.isVisible = false
            speakerViewMenuItem.isVisible = false
        } else {
            speakerViewMenuItem.isVisible = false
            gridViewMenuItem.isVisible = true
        }

        swapCameraMenuItem.isVisible = sharedModel.isNecessaryToShowSwapCameraOption()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.swap_camera -> {
                logDebug("Swap camera.")
                VideoCaptureUtils.swapCamera(ChatChangeVideoStreamListener(meetingActivity))
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
                MegaApplication.setSpeakerViewAutomatic(sharedModel.callLiveData.value!!.callId, false)
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

    /**
     * Init Share View Model
     */
    private fun initShareViewModel() {
        sharedModel.chatRoomLiveData.observe(viewLifecycleOwner) {
        }

        sharedModel.callLiveData.observe(viewLifecycleOwner) {
        }

        sharedModel.meetingNameLiveData.observe(viewLifecycleOwner) {
            toolbar.title = it
        }

        sharedModel.meetingSubtitleLiveData.observe(viewLifecycleOwner) {
            toolbar.subtitle = it
        }

        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            updateLocalAudio(it)
        }

        sharedModel.cameraLiveData.observe(viewLifecycleOwner) {
            updateLocalVideo(it)
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
            BottomFloatingPanelViewHolder(binding, this, isGuest, isModerator)

        /**
         * Observer the participant List
         */
        inMeetingViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participants?.let {
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
    }

    /**
     * Check if a call is outgoing, on hold button must be disabled
     */
    private fun enableOnHoldFab() {
        bottomFloatingPanelViewHolder.enableHoldIcon(!sharedModel.isRequestSent())
    }

    /**
     * Change Cam State
     */
    override fun onChangeCamState(camOn: Boolean) {
        sharedModel.clickCamera(!camOn)

    }

    private fun updateLocalVideo(camOn: Boolean) {
        bottomFloatingPanelViewHolder.updateCamIcon(camOn)

        when (camOn) {
            true -> {
                if (sharedModel.isRequestSent()) {
                    if (individualCallFragment.isAdded) {
                        individualCallFragment.activateVideo()
                    }
                } else {
                    if (floatingWindowFragment.isAdded) {
                        floatingWindowFragment.activateVideo()
                    }
                }
            }
            false -> {
                if (sharedModel.isRequestSent()) {
                    if (individualCallFragment.isAdded) {
                        individualCallFragment.closeVideo()
                    }
                } else {
                    if (floatingWindowFragment.isAdded) {
                        floatingWindowFragment.closeVideo()
                    }
                }
            }
        }
    }

    /**
     * Change Hold State
     */
    override fun onChangeHoldState(isHold: Boolean) {
        inMeetingViewModel.setCallOnHold(sharedModel.chatRoomLiveData.value!!.chatId, isHold)
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
            sharedModel.isOneToOneCall() -> {
                inMeetingViewModel.leaveMeeting(sharedModel.chatRoomLiveData.value!!.chatId)
                meetingActivity.finish()
            }
            isModerator -> {
                val endMeetingBottomSheetDialogFragment =
                    EndMeetingBottomSheetDialogFragment.newInstance()
                endMeetingBottomSheetDialogFragment.show(
                    parentFragmentManager,
                    endMeetingBottomSheetDialogFragment.tag
                )
            }
            else -> {
                askConfirmationEndMeetingForUser()
            }
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
                inMeetingViewModel.leaveMeeting(sharedModel.chatRoomLiveData.value!!.chatId)
            }
        }
    }

    /**
     * Send share link
     */
    override fun onShareLink() {
        if(sharedModel.isOneToOneCall() || !sharedModel.chatRoomLiveData.value!!.isPublic){
            logError("Error getting the link, it is a private chat")
            return
        }

        meetingActivity.startActivity(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getShareLink())
            type = "text/plain"
        })
    }

    /**
     * Get the special link
     */
    private fun getShareLink(): String {
        return sharedModel.meetingLinkLiveData.value!!
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

        const val ANIMATION_DURATION: Long = 500

        const val TAP_THRESHOLD: Long = 500

        const val TOOLBAR_DY = 300f

        const val FLOATING_BOTTOM_SHEET_DY = 400f
    }
}