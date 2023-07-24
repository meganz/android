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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityOpenLinkBinding
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.listeners.LoadPreviewListener
import mega.privacy.android.app.listeners.QueryRecoveryLinkListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment
import mega.privacy.android.app.presentation.filelink.FileLinkActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.usecase.QuerySignupLinkUseCase
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
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_HANDLE_NODE
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK
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
import mega.privacy.android.app.utils.LinksUtil.requiresTransferSession
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util.decodeURL
import mega.privacy.android.app.utils.Util.matchRegexs
import mega.privacy.android.app.utils.isURLSanitized
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Open link activity
 */
@AndroidEntryPoint
class OpenLinkActivity : PasscodeActivity(), MegaRequestListenerInterface,
    LoadPreviewListener.OnPreviewLoadedCallback {


    /**
     * QuerySignupLinkUseCase injection
     */
    @Inject
    lateinit var querySignupLinkUseCase: QuerySignupLinkUseCase

    /**
     * getFeatureFlagValueUseCase
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * MegaChatRequestHandler injection
     */
    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    private var urlConfirmationLink: String? = null

    private var isLoggedIn = false
    private var needsRefreshSession = false

    private var url: String? = null
    private val viewModel by viewModels<OpenLinkViewModel>()
    private val binding: ActivityOpenLinkBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityOpenLinkBinding.inflate(layoutInflater)
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = intent.dataString
        Timber.d("Original url: $url")

        setContentView(binding.root)

        binding.openLinkError.isVisible = false
        binding.containerAcceptButton.isVisible = false
        binding.containerAcceptButton.setOnClickListener {
            finish()
        }

        url = decodeURL(url)
        isLoggedIn = dbH.credentials != null
        needsRefreshSession = megaApi.rootNode == null

        collectFlows()

        when {
            // If is not a MEGA link, is not a supported link
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
                startActivity(
                    Intent(this, FileLinkActivity::class.java)
                        .putExtra(OPENED_FROM_CHAT, intent.getBooleanExtra(OPENED_FROM_CHAT, false))
                        .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(ACTION_OPEN_MEGA_LINK)
                        .setData(Uri.parse(url))
                )
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
                lifecycleScope.launch {
                    val intent =
                        Intent(this@OpenLinkActivity, FolderLinkComposeActivity::class.java)
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
                        querySignupLinkUseCase.query(it)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                { result ->
                                    Timber.d("Not logged")
                                    startActivity(
                                        Intent(this, LoginActivity::class.java)
                                            .putExtra(VISIBLE_FRAGMENT, CREATE_ACCOUNT_FRAGMENT)
                                            .putExtra(EMAIL, result)
                                    )
                                    finish()
                                },
                                { throwable ->
                                    Timber.e(throwable)
                                }
                            )
                            .addTo(composite)
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
            matchRegexs(url, RESET_PASSWORD_LINK_REGEXS) -> {
                Timber.d("Reset pass url")
                if (isLoggedIn) {
                    if (needsRefreshSession) {
                        Timber.d("Go to Login to fetch nodes")
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                                .setAction(ACTION_RESET_PASS)
                                .setData(Uri.parse(url))
                        )
                    } else {
                        Timber.d("Logged IN")
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .setAction(ACTION_RESET_PASS)
                                .setData(Uri.parse(url))
                        )
                    }
                    finish()
                } else {
                    Timber.d("Not logged")
                    megaApi.queryResetPasswordLink(url, QueryRecoveryLinkListener(this))
                }
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
            matchRegexs(url, CONTACT_LINK_REGEXS) -> { //https://mega.nz/C!
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

            else -> {
                // Browser open the link which does not require app to handle
                Timber.d("Browser open link: $url")
                url?.let {
                    checkIfRequiresTransferSession(it)
                }
            }
        }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { openLinkState: OpenLinkState ->
            if (openLinkState.isLoggedOut) {
                startActivity(
                    Intent(this@OpenLinkActivity, LoginActivity::class.java)
                        .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
                        .putExtra(EXTRA_CONFIRMATION, urlConfirmationLink)
                        .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                        .setAction(ACTION_CONFIRM)
                )
                finish()
            }
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
    private fun goToChatActivity() {
        startActivity(
            Intent(this, ChatActivity::class.java)
                .setAction(ACTION_OPEN_CHAT_LINK)
                .setData(Uri.parse(url))
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
     */
    private fun goToMeetingActivity(chatId: Long, meetingName: String) =
        CallUtil.openMeetingGuestMode(
            this,
            meetingName,
            chatId,
            url,
            passcodeManagement,
            chatRequestHandler
        )

    /**
     * Check the url if requires transfer session, if yes open web link.
     */
    private fun checkIfRequiresTransferSession(url: String) {
        if (!requiresTransferSession(this, url)) {
            openWebLink(url)
        }
    }

    /**
     * Open web link
     *
     * @param url web link
     */
    fun openWebLink(url: String?) =
        url?.let {
            startActivity(
                Intent(this, WebViewActivity::class.java)
                    .setData(Uri.parse(it))
            )
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
     * onRequestStart
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart")
    }

    /**
     * onRequestUpdate
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    /**
     * onRequestFinish
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        when (request.type) {
            MegaRequest.TYPE_QUERY_SIGNUP_LINK -> {
                Timber.d("MegaRequest.TYPE_QUERY_SIGNUP_LINK")
                if (e.errorCode == MegaError.API_OK) {
                    MegaApplication.urlConfirmationLink = request.link
                    viewModel.logout()
                } else {
                    setError(getString(R.string.invalid_link))
                }
            }

            else -> {}
        }
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
     * onRequestTemporaryError
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {}

    /**
     * onPreviewLoaded
     */
    override fun onPreviewLoaded(request: MegaChatRequest, alreadyExist: Boolean) {
        val chatId = request.chatHandle
        val isFromOpenChatPreview = request.flag
        val type = request.paramType
        val linkInvalid = TextUtil.isTextEmpty(request.link) && chatId == MEGACHAT_INVALID_HANDLE
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
                    passcodeManagement
                )
            } else {
                when {
                    CallUtil.isMeetingEnded(request.megaHandleList) -> {
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
                        goToMeetingActivity(chatId, request.text)
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
            goToChatActivity()
        }
    }

    /**
     * onErrorLoadingPreview
     */
    override fun onErrorLoadingPreview(errorCode: Int) {
        setError(getString(R.string.invalid_link))
    }
}