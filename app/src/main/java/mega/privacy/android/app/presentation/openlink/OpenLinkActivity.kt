package mega.privacy.android.app.presentation.openlink

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.MegaActivity.Companion.ACTION_DEEP_LINKS
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityOpenLinkBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.listeners.LoadPreviewListener
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.usecase.orientation.enableAdaptiveLayout
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.participatingInACall
import mega.privacy.android.app.utils.CallUtil.showConfirmationInACall
import mega.privacy.android.app.utils.Constants.ACTION_CONFIRM
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_CHAT_LINK
import mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS
import mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_MEETING_LINK
import mega.privacy.android.app.utils.Constants.CREATE_ACCOUNT_FRAGMENT
import mega.privacy.android.app.utils.Constants.EMAIL
import mega.privacy.android.app.utils.Constants.EXTRA_CONFIRMATION
import mega.privacy.android.app.utils.Constants.LINK_IS_FOR_MEETING
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequest
import timber.log.Timber
import javax.inject.Inject

/**
 * Open link activity
 */
@AndroidEntryPoint
class OpenLinkActivity : PasscodeActivity(), LoadPreviewListener.OnPreviewLoadedCallback {

    /**
     * MegaNavigator injection
     */
    @Inject
    lateinit var navigator: MegaNavigator

    /**
     * MegaChatRequestHandler injection
     */
    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    /**
     * Use case to check for current user's email
     */
    @Inject
    lateinit var getCurrentUserEmail: GetCurrentUserEmail

    private var urlConfirmationLink: String? = null
    private var url: String? = null

    private val viewModel by viewModels<OpenLinkViewModel>()
    private val binding: ActivityOpenLinkBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityOpenLinkBinding.inflate(layoutInflater)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        consumeIntentDataDestination()
    }

    private fun consumeIntentDataDestination() {
        viewModel.decodeUri()
    }

    companion object {
        fun getIntent(context: Context, link: Uri) =
            Intent(context, OpenLinkActivity::class.java).apply {
                data = link
                flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            }
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()

        // Set orientation before super.onCreate() to ensure it takes effect
        enableAdaptiveLayout { old, new ->
            Timber.d("On size change in OpenLinkActivity from $old to $new")
        }

        super.onCreate(savedInstanceState)
        url = intent.dataString
        Timber.d("Original url: $url")
        setContentView(binding.root)
        binding.openLinkError.isVisible = false
        binding.containerAcceptButton.isVisible = false
        binding.containerAcceptButton.setOnClickListener {
            finish()
        }

        collectFlow(viewModel.uiState) {
            with(it) {

                handleAccountInvitationEmailState(null)

                if (logoutCompletedEvent) {
                    handleLoggedOutState()
                    viewModel.onLogoutCompletedEventConsumed()
                }

                if (navigateToSingleActivity) {
                    val isFromHistory =
                        (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
                    MegaActivity.getIntent(
                        this@OpenLinkActivity,
                        action = if (isFromHistory) ACTION_MAIN else ACTION_DEEP_LINKS,
                    ).also { megaActivityIntent ->
                        if (!isFromHistory) {
                            megaActivityIntent.data = intent.data
                        }
                        startActivity(megaActivityIntent)
                    }
                    intent = Intent(intent).apply {
                        action = ACTION_VIEW
                        data = null
                        replaceExtras(Bundle())
                    }
                    finish()
                }
            }
        }

        consumeIntentDataDestination()
    }

    private fun navigateToResetPassword(isLoggedIn: Boolean, needsRefreshSession: Boolean) {
        if (isLoggedIn && !needsRefreshSession) {
            Timber.d("Logged IN")
            startActivity(
                Intent(this, MegaActivity::class.java)
                    .setAction(ACTION_RESET_PASS)
                    .setData(Uri.parse(url))
            )
        } else {
            Timber.d("Go to Login to fetch nodes")
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                    .setAction(ACTION_RESET_PASS)
                    .putExtra(LoginActivity.EXTRA_IS_LOGGED_IN, isLoggedIn)
                    .setData(Uri.parse(url))
            )
        }
    }

    /**
     * Handle the isLoggedOut state from [OpenLinkUiState]
     *
     * Navigates to [MegaActivity] if the user logged out
     */
    private fun handleLoggedOutState() = lifecycleScope.launch {
        startActivity(
            Intent(this@OpenLinkActivity, MegaActivity::class.java)
                .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                .putExtra(EXTRA_CONFIRMATION, urlConfirmationLink)
                .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(ACTION_CONFIRM)
        )
        finish()
    }

    /**
     * Navigates to [LoginActivity] if the user navigated from the new signup link
     *
     * Need to check if the email is NULL as the base case which indicates that the user
     * is not from the new signup link, because NULL is the default state in [OpenLinkUiState]
     */
    private fun handleAccountInvitationEmailState(email: String?) {
        email?.let {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(VISIBLE_FRAGMENT, CREATE_ACCOUNT_FRAGMENT)
                    .putExtra(EMAIL, it)
            )
            finish()
        }
    }

    /**
     * Navigate to ChatActivity
     */
    private fun goToChatActivity(chatId: Long) {
        navigator.openChat(
            context = this,
            action = ACTION_OPEN_CHAT_LINK,
            link = url,
            chatId = chatId
        )
        finish()
    }

    /**
     * Navigate to LeftMeetingActivity
     */
    private fun goToGuestLeaveMeetingActivity() {
        startActivity(Intent(this, LeftMeetingActivity::class.java))
        finish()
    }

    /**
     * Navigate to MeetingActivity
     *
     * @param chatId chat ID
     * @param meetingName Meeting Name
     * @param isWaitingRoom Flag to check if it's a Waiting Room
     */
    private fun goToMeetingActivity(chatId: Long, meetingName: String, isWaitingRoom: Boolean) {
        CallUtil.openMeetingGuestMode(
            this,
            meetingName,
            chatId,
            url,
            chatRequestHandler,
            isWaitingRoom
        )
        finish()
    }

    /**
     * Open web link and finish current activity
     *
     * @param url web link
     */
    fun openWebLink(url: String?) = url?.let {
        launchUrl(it)
        finish()
    }

    /**
     * Set error message and views
     *
     * @param errorMessage error message
     */
    fun setError(errorMessage: String) {
        binding.openLinkText.isVisible = false
        binding.openLinkBar.isVisible = false
        binding.openLinkError.text = errorMessage
        binding.openLinkError.isVisible = true
        binding.containerAcceptButton.isVisible = true
    }

    /**
     * onPreviewLoaded
     */
    override fun onPreviewLoaded(request: MegaChatRequest, alreadyExist: Boolean) {
        val chatId = request.chatHandle
        val isFromOpenChatPreview = request.flag
        val type = request.paramType
        val linkInvalid = TextUtil.isTextEmpty(request.link) && chatId == MEGACHAT_INVALID_HANDLE
        val waitingRoom = MegaChatApi.hasChatOptionEnabled(
            MegaChatApi.CHAT_OPTION_WAITING_ROOM,
            request.privilege
        )
        Timber.d("Chat id: $chatId, type: $type, flag: $isFromOpenChatPreview")

        if (linkInvalid) {
            setError(getString(sharedR.string.general_invalid_link))
            return
        }
        if (type == LINK_IS_FOR_MEETING) {
            Timber.d("It's a meeting link")
            if (participatingInACall()) {
                showConfirmationInACall(
                    this,
                    getString(sharedR.string.can_only_join_one_call_error_message),
                )
            } else {
                when {
                    CallUtil.isMeetingEnded(request) -> {
                        Timber.d("Meeting has ended, open dialog")
                        MeetingHasEndedDialogFragment(
                            object : MeetingHasEndedDialogFragment.ClickCallback {
                                override fun onViewMeetingChat() {
                                }

                                override fun onLeave() {
                                    goToGuestLeaveMeetingActivity()
                                }

                            },
                            true
                        ).show(supportFragmentManager, MeetingHasEndedDialogFragment.TAG)
                    }

                    isFromOpenChatPreview -> {
                        Timber.d("Meeting is in progress, open join meeting")
                        goToMeetingActivity(chatId, request.text, waitingRoom)
                    }

                    else -> {
                        Timber.d("It's a meeting, open chat preview")
                        Timber.d("openChatPreview")
                        megaChatApi.openChatPreview(
                            url,
                            LoadPreviewListener(
                                this,
                                this,
                                CHECK_LINK_TYPE_MEETING_LINK
                            )
                        )
                    }
                }
            }
        } else {
            Timber.d("It's a chat link")
            goToChatActivity(chatId)
        }
    }

    /**
     * onErrorLoadingPreview
     */
    override fun onErrorLoadingPreview(errorCode: Int) {
        setError(getString(sharedR.string.general_invalid_link))
    }
}
