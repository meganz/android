package mega.privacy.android.app.meeting.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityMeetingBinding
import mega.privacy.android.app.meeting.CallNotificationIntentService
import mega.privacy.android.app.meeting.fragments.CreateMeetingFragment
import mega.privacy.android.app.meeting.fragments.InMeetingFragment
import mega.privacy.android.app.meeting.fragments.JoinMeetingAsGuestFragment
import mega.privacy.android.app.meeting.fragments.JoinMeetingFragment
import mega.privacy.android.app.meeting.fragments.MakeModeratorFragment
import mega.privacy.android.app.meeting.fragments.MeetingBaseFragment
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.view.UsersInWaitingRoomDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID
import mega.privacy.android.app.utils.PasscodeUtil
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MeetingActivity : BaseActivity() {

    companion object {
        /** The name of actions denoting set
        JOIN/CREATE/JOIN AS GUEST/In-meeting screen as the initial screen */
        const val MEETING_ACTION_JOIN = "join_meeting"
        const val MEETING_ACTION_REJOIN = "rejoin_meeting"
        const val MEETING_ACTION_CREATE = "create_meeting"
        const val MEETING_ACTION_GUEST = "join_meeting_as_guest"
        const val MEETING_ACTION_IN = "in_meeting"
        const val MEETING_ACTION_MAKE_MODERATOR = "make_moderator"
        const val MEETING_ACTION_RINGING = "ringing_meeting"
        const val MEETING_ACTION_RINGING_VIDEO_ON = "ringing_meeting_video_on"
        const val MEETING_ACTION_RINGING_VIDEO_OFF = "ringing_meeting_video_off"
        const val MEETING_ACTION_START = "start_meeting"

        /** The names of the Extra data being passed to the initial fragment */
        const val MEETING_NAME = "meeting_name"
        const val MEETING_LINK = "meeting_link"
        const val MEETING_CHAT_ID = "chat_id"
        const val MEETING_PUBLIC_CHAT_HANDLE = "public_chat_handle"
        const val MEETING_AUDIO_ENABLE = "audio_enable"
        const val MEETING_VIDEO_ENABLE = "video_enable"
        const val MEETING_IS_GUEST = "is_guest"
        const val CALL_ACTION = "call_action"

        fun getIntentOngoingCall(context: Context, chatId: Long): Intent {
            return Intent(context, MeetingActivity::class.java).apply {
                putExtra(MEETING_CHAT_ID, chatId)
                action = MEETING_ACTION_IN
            }
        }
    }

    @Inject
    lateinit var passcodeUtil: PasscodeUtil

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Rtc audio manager gateway
     */
    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    lateinit var binding: ActivityMeetingBinding
    private val meetingViewModel: MeetingActivityViewModel by viewModels()
    private val waitingRoomManagementViewModel by viewModels<WaitingRoomManagementViewModel>()

    private var meetingAction: String? = null

    private var isGuest = false
    private var isLockingEnabled = false

    private var navController: NavController? = null
    private var navGraph: NavGraph? = null

    private val destinationChangedListener: NavController.OnDestinationChangedListener by lazy {
        NavController.OnDestinationChangedListener { _, destination, _ ->
            binding.bannerMute.elevation = when (destination.id) {
                R.id.makeModeratorFragment -> 0f
                else -> 1f
            }
        }
    }

    /**
     * Handle events when a Back Press is detected
     */
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentFragment = getCurrentFragment()

            when (currentFragment) {
                is CreateMeetingFragment -> {
                    currentFragment.releaseVideoAndHideKeyboard()
                    removeRTCAudioManager()
                }

                is JoinMeetingFragment -> {
                    currentFragment.releaseVideoDeviceAndRemoveChatVideoListener()
                    removeRTCAudioManager()
                }

                is JoinMeetingAsGuestFragment -> {
                    currentFragment.releaseVideoAndHideKeyboard()
                    removeRTCAudioManager()
                }

                is InMeetingFragment -> {
                    // Prevent guest from quitting the call by pressing back
                    if (!isGuest) {
                        currentFragment.removeUI()
                    }
                }

                is MakeModeratorFragment -> {
                    currentFragment.cancel()
                }
            }

            if (currentFragment !is MakeModeratorFragment && (currentFragment !is InMeetingFragment || !isGuest)) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun View.setMarginTop(marginTop: Int) {
        val menuLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
        menuLayoutParams.setMargins(0, marginTop, 0, 0)
        this.layoutParams = menuLayoutParams
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        intent = newIntent

        initIntent()
        initActionBar()
        initNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup the Back Press dispatcher to receive Back Press events
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initIntent()

        binding = ActivityMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActionBar()
        initNavigation()
        setStatusBarTranslucent()
        binding.waitingRoomDialogComposeView.apply {
            isVisible = true
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                val waitingRoomState by waitingRoomManagementViewModel.state.collectAsStateWithLifecycle()
                AndroidTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog(
                        state = waitingRoomState,
                        onAdmitClick = {
                            waitingRoomManagementViewModel.admitUsersClick()
                        },
                        onDenyClick = {
                            waitingRoomManagementViewModel.denyUsersClick()
                        },
                        onDenyEntryClick = {
                            waitingRoomManagementViewModel.denyEntryClick()
                            waitingRoomManagementViewModel.setShowDenyParticipantDialogConsumed()
                        },
                        onSeeWaitingRoomClick = {
                            waitingRoomManagementViewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                            waitingRoomManagementViewModel.setShowDenyParticipantDialogConsumed()
                        },
                        onDismiss = {
                            waitingRoomManagementViewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                        },
                        onDismissDenyParticipantDialog = {
                            waitingRoomManagementViewModel.setShowDenyParticipantDialogConsumed()
                        },
                    )
                }
            }
        }

        collectFlows()

        collectFlow(meetingViewModel.finishMeetingActivity) { shouldFinish ->
            if (shouldFinish) {
                if (meetingViewModel.amIAGuest()) {
                    meetingViewModel.logout()
                } else {
                    finish()
                }
            }
        }

        collectFlow(meetingViewModel.switchCall) { chatId ->
            if (chatId != MEGACHAT_INVALID_HANDLE && meetingViewModel.currentChatId.value != chatId) {
                Timber.d("Switch call")
                passcodeManagement.showPasscodeScreen = true
                MegaApplication.getInstance().openCallService(chatId)
                startActivity(getIntentOngoingCall(this@MeetingActivity, chatId))
            }
        }
    }

    private fun collectFlows() {
        collectFlow(meetingViewModel.state) { state: MeetingState ->
            if (state.shouldLaunchLeftMeetingActivity) {
                startActivity(
                    Intent(this, LeftMeetingActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }

            if (state.chatId != -1L) {
                waitingRoomManagementViewModel.setChatIdCallOpened(state.chatId)
            }
        }
    }

    override fun onDestroy() {
        navController?.removeOnDestinationChangedListener(destinationChangedListener)

        super.onDestroy()
    }

    private fun initIntent() {
        intent?.let {
            if (it.action == CallNotificationIntentService.ANSWER) {
                it.extras?.let { extra ->
                    val chatIdIncomingCall = extra.getLong(
                        Constants.CHAT_ID_OF_INCOMING_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val answerIntent = Intent(this, CallNotificationIntentService::class.java)
                    answerIntent.putExtra(
                        Constants.CHAT_ID_OF_CURRENT_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )
                    answerIntent.putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, chatIdIncomingCall)
                    answerIntent.action = it.action
                    startService(answerIntent)
                    finish()
                    return
                }
            }

            if (it.action == CallNotificationIntentService.START_SCHED_MEET) {
                it.extras?.let { extra ->
                    val chatIdIncomingCall = extra.getLong(
                        Constants.CHAT_ID_OF_INCOMING_CALL,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val schedIdIncomingCall = extra.getLong(
                        Constants.SCHEDULED_MEETING_ID,
                        MEGACHAT_INVALID_HANDLE
                    )

                    val intent = Intent(this, CallNotificationIntentService::class.java).apply {
                        action = it.action
                        putExtra(Constants.CHAT_ID_OF_CURRENT_CALL, MEGACHAT_INVALID_HANDLE)
                        putExtra(Constants.CHAT_ID_OF_INCOMING_CALL, chatIdIncomingCall)
                        putExtra(Constants.SCHEDULED_MEETING_ID, schedIdIncomingCall)
                    }
                    startService(intent)
                    finish()
                    return
                }
            }
            val chatId = it.getLongExtra(MEETING_CHAT_ID, MEGACHAT_INVALID_HANDLE)
            meetingViewModel.updateChatRoomId(chatId)

            // Cancel current notification if needed
            notificationManager.cancel(chatId.toInt())

            isGuest = it.getBooleanExtra(
                MEETING_IS_GUEST,
                false
            )

            if ((!isGuest && shouldRefreshSessionDueToSDK()) || shouldRefreshSessionDueToKarere()) {
                meetingViewModel.currentChatId.value?.let { currentChatId ->
                    if (currentChatId != MEGACHAT_INVALID_HANDLE) {
                        //Notification of this call should be displayed again
                        MegaApplication.getChatManagement().removeNotificationShown(currentChatId)
                    }
                }

                return
            }

            meetingAction = it.action
        }
    }

    @Suppress("DEPRECATION")
    private fun setStatusBarTranslucent() {
        val decorView: View = window.decorView

        decorView.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets? ->
            val defaultInsets = v.onApplyWindowInsets(insets)

            binding.toolbar.setMarginTop(defaultInsets.systemWindowInsetTop)

            defaultInsets.replaceSystemWindowInsets(
                defaultInsets.systemWindowInsetLeft,
                0,
                defaultInsets.systemWindowInsetRight,
                defaultInsets.systemWindowInsetBottom
            )
        }

        ViewCompat.requestApplyInsets(decorView)
    }

    /**
     * Initialize Action Bar and set icon according to param
     */
    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar ?: return
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = ""

        when (meetingAction) {
            MEETING_ACTION_CREATE, MEETING_ACTION_JOIN, MEETING_ACTION_GUEST -> {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white)
            }

            MEETING_ACTION_IN -> actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }
    }

    /**
     * Initialize Navigation and set startDestination(initial screen)
     * according to the meeting action
     */
    private fun initNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navGraph?.clear()
        navGraph = navHostFragment.navController.navInflater.inflate(R.navigation.meeting)

        // The args to be passed to startDestination
        val bundle = Bundle()

        meetingViewModel.currentChatId.value?.let { currentChatId ->
            bundle.putLong(
                MEETING_CHAT_ID,
                currentChatId
            )
        }

        bundle.putLong(
            MEETING_PUBLIC_CHAT_HANDLE,
            intent.getLongExtra(MEETING_PUBLIC_CHAT_HANDLE, MEGACHAT_INVALID_HANDLE)
        )

        // Pass the meeting data to Join Meeting screen
        if (meetingAction == MEETING_ACTION_GUEST || meetingAction == MEETING_ACTION_JOIN) {
            bundle.putString(MEETING_LINK, intent.dataString)
            bundle.putString(MEETING_NAME, intent.getStringExtra(MEETING_NAME))
        }

        if (meetingAction == MEETING_ACTION_IN) {
            bundle.putBoolean(
                MEETING_AUDIO_ENABLE, intent.getBooleanExtra(
                    MEETING_AUDIO_ENABLE,
                    false
                )
            )
            bundle.putBoolean(
                MEETING_VIDEO_ENABLE, intent.getBooleanExtra(
                    MEETING_VIDEO_ENABLE,
                    false
                )
            )

            bundle.putBoolean(
                MEETING_IS_GUEST,
                isGuest
            )
        }

        navGraph?.apply {
            val startDestination = when (meetingAction) {
                MEETING_ACTION_CREATE -> R.id.createMeetingFragment
                MEETING_ACTION_JOIN, MEETING_ACTION_REJOIN -> R.id.joinMeetingFragment
                MEETING_ACTION_GUEST -> R.id.joinMeetingAsGuestFragment
                MEETING_ACTION_START, MEETING_ACTION_IN -> R.id.inMeetingFragment
                MEETING_ACTION_RINGING -> R.id.ringingMeetingFragment
                MEETING_ACTION_MAKE_MODERATOR -> R.id.makeModeratorFragment
                else -> R.id.createMeetingFragment
            }
            setStartDestination(startDestination)

            navController?.setGraph(this, bundle)
        }

        navController?.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Get current fragment from navHostFragment
     */
    fun getCurrentFragment(): MeetingBaseFragment? {
        val navHostFragment: Fragment? =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        return navHostFragment?.childFragmentManager?.fragments?.get(0) as MeetingBaseFragment?
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        meetingViewModel.inviteToChat(this, requestCode, resultCode, intent)
        super.onActivityResult(requestCode, resultCode, intent)
    }

    fun showSnackbar(content: String) {
        showSnackbar(binding.navHostFragment, content)
    }

    override fun onStop() {
        super.onStop()
        val currentFragment = getCurrentFragment()
        if (currentFragment is InMeetingFragment) {
            meetingViewModel.sendEnterCallEvent(false)
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            val timeRequired = passcodeUtil.timeRequiredForPasscode()
            if (timeRequired != REQUIRE_PASSCODE_INVALID) {
                if (isLockingEnabled) {
                    passcodeManagement.lastPause = System.currentTimeMillis() - timeRequired
                } else {
                    passcodeUtil.pauseUpdate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            isLockingEnabled = passcodeUtil.shouldLock()
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or 0x00000010

        val currentFragment = getCurrentFragment()
        if (currentFragment is InMeetingFragment) {
            meetingViewModel.sendEnterCallEvent(true)
        }
    }

    /**
     * Method to remove the RTC Audio Manager when the call has not been finally established
     */
    private fun removeRTCAudioManager() {
        if (!meetingViewModel.isChatCreatedAndIParticipating()) {
            rtcAudioManagerGateway.removeRTCAudioManager()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (rtcAudioManagerGateway.isAnIncomingCallRinging) {
                    rtcAudioManagerGateway.muteOrUnMute(false)
                }
                false
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (rtcAudioManagerGateway.isAnIncomingCallRinging) {
                    rtcAudioManagerGateway.muteOrUnMute(true)
                }
                false
            }

            else -> super.dispatchKeyEvent(event)
        }
    }
}