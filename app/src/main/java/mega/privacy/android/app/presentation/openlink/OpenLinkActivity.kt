package mega.privacy.android.app.presentation.openlink

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityOpenLinkBinding
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.listeners.LoadPreviewListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.participatingInACall
import mega.privacy.android.app.utils.CallUtil.showConfirmationInACall
import mega.privacy.android.app.utils.Constants.ACCOUNT_INVITATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.ACTION_CANCEL_ACCOUNT
import mega.privacy.android.app.utils.Constants.ACTION_CHANGE_MAIL
import mega.privacy.android.app.utils.Constants.ACTION_CHAT_SUMMARY
import mega.privacy.android.app.utils.Constants.ACTION_CONFIRM
import mega.privacy.android.app.utils.Constants.ACTION_EXPORT_MASTER_KEY
import mega.privacy.android.app.utils.Constants.ACTION_IPC
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_CHAT_LINK
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_CONTACTS_SECTION
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_DEVICE_CENTER
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_HANDLE_NODE
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_SYNC_MEGA_FOLDER
import mega.privacy.android.app.utils.Constants.ACTION_RESET_PASS
import mega.privacy.android.app.utils.Constants.ALBUM_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.BUSINESS_INVITE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CHAT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_MEETING_LINK
import mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_UNKNOWN_LINK
import mega.privacy.android.app.utils.Constants.CONFIRMATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CONTACT_HANDLE
import mega.privacy.android.app.utils.Constants.CONTACT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CREATE_ACCOUNT_FRAGMENT
import mega.privacy.android.app.utils.Constants.EMAIL
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.EXPORT_MASTER_KEY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.EXTRA_CONFIRMATION
import mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.HANDLE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.LINK_IS_FOR_MEETING
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.MEGA_BLOG_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.MEGA_DROP_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.MEGA_FILE_REQUEST_LINK_REGEXES
import mega.privacy.android.app.utils.Constants.NEW_MESSAGE_CHAT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.OPENED_FROM_CHAT
import mega.privacy.android.app.utils.Constants.PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.PENDING_CONTACTS_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.REVERT_CHANGE_PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.Constants.WEB_SESSION_LINK_REGEXS
import mega.privacy.android.app.utils.ConstantsUrl.recoveryUrl
import mega.privacy.android.app.utils.LinksUtil.requiresTransferSession
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.matchRegexs
import mega.privacy.android.app.utils.isURLSanitized
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.DeeplinkHandler
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiAndroid
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
     * [DeeplinkHandler] injection
     */
    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    /**
     * MegaChatRequestHandler injection
     */
    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    /**
     * Use case to check for matches in Regex Patterns
     */
    @Inject
    lateinit var getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase

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

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
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
                url = decodedUrl
                if (urlRedirectionEvent && isLoggedIn != null) {
                    handleUrlRedirection(
                        isLoggedIn = isLoggedIn,
                        needsRefreshSession = needsRefreshSession
                    )
                    viewModel.onUrlRedirectionEventConsumed()
                }

                if (logoutCompletedEvent) {
                    handleLoggedOutState()
                    handleAccountInvitationEmailState(accountInvitationEmail)
                    viewModel.onLogoutCompletedEventConsumed()
                }

                if (resetPasswordLinkResult != null) {
                    if (resetPasswordLinkResult.isSuccess) {
                        val emailForLink = resetPasswordLinkResult.getOrThrow()
                        if (emailForLink != userEmail && isLoggedIn == true) {
                            setError(getString(R.string.error_not_logged_with_correct_account))
                        } else {
                            navigateToResetPassword(isLoggedIn == true, needsRefreshSession)
                            finish()
                        }
                    } else {
                        val errorMessage = when (resetPasswordLinkResult.exceptionOrNull()) {
                            ResetPasswordLinkException.LinkInvalid -> getString(R.string.invalid_link)
                            ResetPasswordLinkException.LinkExpired -> getString(R.string.recovery_link_expired)
                            ResetPasswordLinkException.LinkAccessDenied -> getString(R.string.error_not_logged_with_correct_account)
                            else -> getString(R.string.general_text_error)
                        }
                        setError(errorMessage)
                    }
                    viewModel.onResetPasswordLinkResultConsumed()
                }
            }
        }

        url?.let { viewModel.decodeUrl(it) }
    }

    private fun handleUrlRedirection(isLoggedIn: Boolean, needsRefreshSession: Boolean) {
        when {
            // Check if it is a supported link
            !url.isURLSanitized() -> {
                Timber.d("OpenLinkActivity: URL doesn't match regex pattern or whitelisted $url")
                setError(getString(R.string.open_link_not_valid_link))
            }
            // Album link
            matchRegexs(url, ALBUM_LINK_REGEXS) -> {
                lifecycleScope.launch {
                    val intent = AlbumScreenWrapperActivity.createAlbumImportScreen(
                        context = this@OpenLinkActivity,
                        albumLink = AlbumLink(url.orEmpty()),
                    ).apply {
                        flags = FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                    finish()
                }
            }
            // Email verification link
            matchRegexs(url, EMAIL_VERIFY_LINK_REGEXS) -> {
                Timber.d("Open email verification link")
                MegaApplication.setIsWebOpenDueToEmailVerification(true)
                openWebLink(url)
            }
            // Web session link
            matchRegexs(url, WEB_SESSION_LINK_REGEXS) -> {
                Timber.d("Open web session link")
                openWebLink(url)
            }

            matchRegexs(url, BUSINESS_INVITE_LINK_REGEXS) -> {
                Timber.d("Open business invite link")
                openWebLink(url)
            }
            //MEGA DROP link
            matchRegexs(url, MEGA_DROP_LINK_REGEXS) -> {
                Timber.d("Open MEGA drop link")
                openWebLinkInBrowser(url)
            }
            //MEGA File Request
            matchRegexs(url, MEGA_FILE_REQUEST_LINK_REGEXES) -> {
                Timber.d("Open MEGA file request link")
                openWebLinkInBrowser(url)
            }
            // File link
            matchRegexs(url, FILE_LINK_REGEXS) -> {
                Timber.d("Open link url")
                val intent =
                    Intent(this@OpenLinkActivity, FileLinkComposeActivity::class.java).apply {
                        putExtra(
                            OPENED_FROM_CHAT,
                            intent.getBooleanExtra(OPENED_FROM_CHAT, false)
                        )
                        flags = FLAG_ACTIVITY_CLEAR_TOP
                        action = ACTION_OPEN_MEGA_LINK
                        data = Uri.parse(url)
                    }
                startActivity(intent)
                finish()
            }
            // Confirmation link
            matchRegexs(url, CONFIRMATION_LINK_REGEXS) -> {
                Timber.d("Confirmation url")
                urlConfirmationLink = url
                MegaApplication.urlConfirmationLink = urlConfirmationLink
                viewModel.logout()
            }
            // Folder Download link
            matchRegexs(url, FOLDER_LINK_REGEXS) -> {
                val intent = Intent(this@OpenLinkActivity, FolderLinkComposeActivity::class.java)
                startActivity(
                    intent
                        .putExtra(
                            OPENED_FROM_CHAT,
                            intent.getBooleanExtra(OPENED_FROM_CHAT, false)
                        )
                        .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(ACTION_OPEN_MEGA_FOLDER_LINK)
                        .setData(Uri.parse(url))
                )
                finish()
            }
            // Chat link or Meeting link
            matchRegexs(url, CHAT_LINK_REGEXS) -> {
                Timber.d("Open chat url")
                if (isLoggedIn) {
                    Timber.d("Logged IN")
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .setAction(ACTION_OPEN_CHAT_LINK)
                            .setData(Uri.parse(url))
                    )
                    finish()
                } else {
                    Timber.d("Not logged")
                    var initResult = megaChatApi.initState
                    if (initResult < MegaChatApi.INIT_WAITING_NEW_SESSION) {
                        initResult = megaChatApi.initAnonymous()
                        Timber.d("Chat init anonymous result: $initResult")
                    }
                    if (initResult != MegaChatApi.INIT_ERROR) {
                        finishAfterConnect()
                    } else {
                        Timber.e("Open chat url:initAnonymous:INIT_ERROR")
                        setError(getString(R.string.error_chat_link_init_error))
                    }
                }
            }
            // Password link
            matchRegexs(url, PASSWORD_LINK_REGEXS) -> {
                Timber.d("Link with password url")
                startActivity(
                    Intent(this, OpenPasswordLinkActivity::class.java)
                        .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                        .setData(Uri.parse(url))
                )
                finish()
            }
            // Create account invitation - user must be logged OUT
            matchRegexs(url, ACCOUNT_INVITATION_LINK_REGEXS) -> {
                Timber.d("New signup url")
                if (isLoggedIn) {
                    Timber.d("Logged IN")
                    setError(getString(R.string.log_out_warning))
                } else {
                    url?.let {
                        viewModel.getAccountInvitationEmail(it)
                    }
                }
            }
            // Export Master Key link - user must be logged IN
            matchRegexs(url, EXPORT_MASTER_KEY_LINK_REGEXS) -> {
                Timber.d("Export master key url")
                if (isLoggedIn) { //Check fetch nodes is already done in ManagerActivity
                    Timber.d("Logged IN")
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .setAction(ACTION_EXPORT_MASTER_KEY)
                    )
                    finish()
                } else {
                    Timber.d("Not logged")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }
            // New message chat- user must be logged IN
            matchRegexs(url, NEW_MESSAGE_CHAT_LINK_REGEXS) -> {
                Timber.d("New message chat url")
                if (isLoggedIn) { //Check fetch nodes is already done in ManagerActivity
                    Timber.d("Logged IN")
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .setAction(ACTION_CHAT_SUMMARY)
                    )
                    finish()
                } else {
                    Timber.d("Not logged")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }
            // Cancel account  - user must be logged IN
            matchRegexs(url, CANCEL_ACCOUNT_LINK_REGEXS) -> {
                Timber.d("Cancel account url")
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes")
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                                .setAction(ACTION_CANCEL_ACCOUNT)
                                .setData(Uri.parse(url))
                        )
                    } else {
                        Timber.d("Logged IN")
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .setAction(ACTION_CANCEL_ACCOUNT)
                                .setData(Uri.parse(url))
                        )
                    }
                    finish()
                } else {
                    Timber.d("Not logged")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }
            // Verify change mail - user must be logged IN
            matchRegexs(url, VERIFY_CHANGE_MAIL_LINK_REGEXS) -> {
                Timber.d("Verify mail url")
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes")
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                                .setAction(ACTION_CHANGE_MAIL)
                                .setData(Uri.parse(url))
                        )
                    } else {
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .setAction(ACTION_CHANGE_MAIL)
                                .setData(Uri.parse(url))
                        )
                    }
                    finish()
                } else {
                    setError(getString(R.string.change_email_not_logged_in))
                }
            }
            // Reset password - two options: logged IN or OUT
            matchRegexs(url, RESET_PASSWORD_LINK_REGEXS) && matchesRecoveryUrl(url).not() -> {
                Timber.d("Reset pass url")
                viewModel.queryResetPasswordLink(url.orEmpty())
            }
            // Pending contacts
            matchRegexs(url, PENDING_CONTACTS_LINK_REGEXS) -> {
                Timber.d("Pending contacts url")
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes")
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                                .setAction(ACTION_IPC)
                        )
                    } else {
                        Timber.d("Logged IN")
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .setAction(ACTION_IPC)
                        )
                    }
                    finish()
                } else {
                    Timber.w("Not logged")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            matchRegexs(url, REVERT_CHANGE_PASSWORD_LINK_REGEXS)
                    || matchRegexs(url, MEGA_BLOG_LINK_REGEXS) -> {
                Timber.d("Open revert password change link: $url")
                openWebLink(url)
            }

            // Open Sync folder link
            getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.OPEN_SYNC_MEGA_FOLDER_LINK -> {
                if (isLoggedIn) {
                    Timber.d("Open sync folder link")
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .setAction(ACTION_OPEN_SYNC_MEGA_FOLDER)
                            .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                            .setData(Uri.parse(url))
                    )
                    finish()
                } else {
                    Timber.w("Not logged in")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            matchRegexs(url, HANDLE_LINK_REGEXS) -> {
                Timber.d("Handle link url")
                startActivity(
                    Intent(this, ManagerActivity::class.java)
                        .setAction(ACTION_OPEN_HANDLE_NODE)
                        .setData(Uri.parse(url))
                )
                finish()
            }
            //Contact link
            matchRegexs(url, CONTACT_LINK_REGEXS) -> {
                if (isLoggedIn) {
                    url?.split("C!")?.get(1)?.let {
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .setAction(ACTION_OPEN_CONTACTS_SECTION)
                                .putExtra(CONTACT_HANDLE, MegaApiAndroid.base64ToHandle(it))
                        )
                        finish()
                    } ?: let {
                        // Browser open the link which does not require app to handle
                        Timber.d("Browser open link: $url")
                        url?.let {
                            checkIfRequiresTransferSession(it)
                        }
                    }
                } else {
                    Timber.w("Not logged")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            getUrlRegexPatternTypeUseCase(url) == RegexPatternType.INSTALLER_DOWNLOAD_LINK -> {
                Timber.d("INSTALLER_DOWNLOAD_LINK $url")
                openWebLinkInBrowser(url)
            }

            getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.PURCHASE_LINK -> {
                Timber.d("PURCHASE_LINK $url")
                openWebLinkInBrowser(url)
            }

            //Upgrade Account link
            getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.UPGRADE_PAGE_LINK
                    || getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.UPGRADE_LINK -> {
                lifecycleScope.launch {
                    if (isLoggedIn) {
                        navigateToUpgradeAccount()
                        finish()
                    } else {
                        Timber.w("Not logged in")
                        setError(getString(R.string.alert_not_logged_in))
                    }
                }
            }
            //Enable camera uploads link
            getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK -> {
                if (isLoggedIn) {
                    navigator.openSettingsCameraUploads(this)
                    finish()
                } else {
                    Timber.w("Not logged in")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            // Open Device Center Link
            getUrlRegexPatternTypeUseCase(url?.lowercase()) == RegexPatternType.OPEN_DEVICE_CENTER_LINK -> {
                if (isLoggedIn) {
                    Timber.d("Open device center link")
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .setAction(ACTION_OPEN_DEVICE_CENTER)
                            .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                } else {
                    Timber.w("Not logged in")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            deeplinkHandler.matches(url.toString()) -> {
                if (isLoggedIn) {
                    deeplinkHandler.process(this, url.toString())
                    finish()
                } else {
                    Timber.w("Not logged in")
                    setError(getString(R.string.alert_not_logged_in))
                }
            }

            else -> {
                // Browser open the link which does not require app to handle
                Timber.d("Browser open link: $url")
                url?.let {
                    checkIfRequiresTransferSession(it)
                }
            }
        }
    }

    /**
     * Check if the provided url matches recovery url
     */
    private fun matchesRecoveryUrl(url: String?) =
        url == recoveryUrl(MEGA_NZ_DOMAIN_NAME)
                || url == recoveryUrl(MEGA_APP_DOMAIN_NAME)

    private fun navigateToResetPassword(isLoggedIn: Boolean, needsRefreshSession: Boolean) {
        if (isLoggedIn && !needsRefreshSession) {
            Timber.d("Logged IN")
            startActivity(
                Intent(this, ManagerActivity::class.java)
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
     * Navigates to [LoginActivity] if the user logged out
     */
    private fun handleLoggedOutState() = lifecycleScope.launch {
        getFeatureFlagValueUseCase(AppFeatures.SingleActivity).let { isSingleActivityEnabled ->
            Timber.d("SingleActivity feature flag is enabled: $isSingleActivityEnabled")
            val targetActivity = if (isSingleActivityEnabled) {
                MegaActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(
                Intent(this@OpenLinkActivity, targetActivity)
                    .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                    .putExtra(EXTRA_CONFIRMATION, urlConfirmationLink)
                    .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                    .setAction(ACTION_CONFIRM)
            )
            finish()
        }
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
     * Finish after Connect
     */
    private fun finishAfterConnect() =
        megaChatApi.checkChatLink(
            url,
            LoadPreviewListener(this, this, CHECK_LINK_TYPE_UNKNOWN_LINK)
        )

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
     * Check the url if requires transfer session, if yes open web link.
     */
    private fun checkIfRequiresTransferSession(url: String) {
        if (!requiresTransferSession(this, url)) {
            openWebLink(url)
        }
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

    private fun openWebLinkInBrowser(url: String?) = url?.let {
        val intent = Intent(ACTION_VIEW).apply { data = Uri.parse(url) }

        // On Android 12+ devices, Intent.createChooser cannot properly show browser list.
        // So workaround here: get list of browsers and insert into initial list of
        // chooser.
        val initialBrowserList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val browserActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            browserActivities
                .filterNot { it.activityInfo.packageName.contains(packageName) }
                .map {
                    Intent(ACTION_VIEW, Uri.parse(url)).apply {
                        `package` = it.activityInfo.packageName
                    }
                }.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val chooserIntent = Intent.createChooser(intent, null).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(
                    ComponentName(this@OpenLinkActivity, OpenLinkActivity::class.java)
                )
            )

            initialBrowserList?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, it.toTypedArray())
            }
        }
        startActivity(chooserIntent)
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
            setError(getString(R.string.invalid_link))
            return
        }
        if (type == LINK_IS_FOR_MEETING) {
            Timber.d("It's a meeting link")
            if (participatingInACall()) {
                showConfirmationInACall(
                    this,
                    getString(R.string.text_join_call),
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
        setError(getString(R.string.invalid_link))
    }
}