package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication.Companion.getChatManagement
import mega.privacy.android.app.MegaApplication.Companion.isIsHeartBeatAlive
import mega.privacy.android.app.MegaApplication.Companion.setHeartBeatAlive
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginViewModel.Companion.ACTION_FORCE_RELOAD_ACCOUNT
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.NewLoginView
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil.setStartScreenTimeStamp
import mega.privacy.android.app.providers.FileProviderActivity
import mega.privacy.android.app.upgradeAccount.ChooseAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.LAUNCH_INTENT
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL_EMAIL
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LoginScreenEvent
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Login fragment.
 *
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject
    @LoginMutex
    lateinit var loginMutex: Mutex

    private val viewModel: LoginViewModel by activityViewModels()

    private val billingViewModel by activityViewModels<BillingViewModel>()

    private var intentExtras: Bundle? = null
    private var intentData: Uri? = null
    private var intentAction: String? = null
    private var intentDataString: String? = null
    private var intentParentHandle: Long = -1
    private var intentShareInfo: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { LoginScreen() }
    }

    @Composable
    private fun LoginScreen() {
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var showIncorrectRkDialog by rememberSaveable { mutableStateOf(false) }
        var recoveryKeyInput by rememberSaveable(uiState.recoveryKeyLink) { mutableStateOf("") }
        var recoveryKeyError by rememberSaveable(uiState.recoveryKeyLink) {
            mutableStateOf<String?>(null)
        }

        EventEffect(uiState.checkRecoveryKeyEvent, viewModel::onCheckRecoveryKeyEventConsumed) {
            if (it.isSuccess) {
                val data = it.getOrThrow()
                navigateToChangePassword(link = data.link, value = data.recoveryKey)
            } else {
                val e = it.exceptionOrNull()
                Timber.e(e)
                when ((e as? MegaException)?.errorCode ?: Int.MIN_VALUE) {
                    MegaError.API_EKEY -> showIncorrectRkDialog = true
                    MegaError.API_EBLOCKED -> viewModel.setSnackbarMessageId(R.string.error_reset_account_blocked)
                    else -> viewModel.setSnackbarMessageId(R.string.general_text_error)
                }
            }
        }

        EventEffect(uiState.onBackPressedEvent, viewModel::consumedOnBackPressedEvent) {
            onBackPressed(uiState)
        }

        LaunchedEffect(uiState.intentState) {
            uiState.intentState?.let {
                when (it) {
                    LoginIntentState.ReadyForInitialSetup -> {
                        Timber.d("Ready to initial setup")
                        finishSetupIntent(uiState)
                    }

                    LoginIntentState.ReadyForFinalSetup -> {
                        Timber.d("Ready to finish")
                        readyToFinish(uiState)
                    }

                    else -> {
                        /* Nothing to update */
                        Timber.d("Intent state: $this")
                    }
                }
            }
        }

        AndroidTheme(isDark = uiState.themeMode.isDarkMode()) {
            LaunchedEffect(Unit) {
                Analytics.tracker.trackEvent(LoginScreenEvent)
            }
            NewLoginView(
                state = uiState,
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onLoginClicked = {
                    LoginActivity.isBackFromLoginPage = false
                    viewModel.onLoginClicked(false)
                    billingViewModel.loadSkus()
                    billingViewModel.loadPurchases()
                },
                onForgotPassword = { onForgotPassword(context, uiState.accountSession?.email) },
                onCreateAccount = {
                    viewModel.setPendingFragmentToShow(LoginFragmentType.CreateAccount)
                },
                onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
                on2FAChanged = viewModel::on2FAChanged,
                onLostAuthenticatorDevice = { onLostAuthenticationDevice(context) },
                onBackPressed = { onBackPressed(uiState) },
                onReportIssue = ::openLoginIssueHelpdeskPage,
                onLoginExceptionConsumed = viewModel::setLoginErrorConsumed,
                onResetAccountBlockedEvent = viewModel::resetAccountBlockedEvent,
                onResendVerificationEmail = viewModel::resendVerificationEmail,
                onResetResendVerificationEmailEvent = viewModel::resetResendVerificationEmailEvent,
                stopLogin = viewModel::stopLogin,
            )

            if (uiState.ongoingTransfersExist == true) {
                BasicDialog(
                    title = "",
                    description = stringResource(id = R.string.login_warning_abort_transfers),
                    positiveButtonText = stringResource(id = sharedR.string.login_text),
                    onPositiveButtonClicked = {
                        viewModel.onLoginClicked(true)
                        viewModel.resetOngoingTransfers()
                    },
                    negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    onNegativeButtonClicked = {
                        viewModel.resetOngoingTransfers()
                    },
                    dismissOnClickOutside = false,
                    dismissOnBackPress = false
                )
            }

            if (showIncorrectRkDialog) {
                BasicDialog(
                    title = stringResource(id = sharedR.string.recovery_key_error_title),
                    description = stringResource(id = sharedR.string.recovery_key_error_description),
                    positiveButtonText = stringResource(id = R.string.general_ok),
                    onPositiveButtonClicked = {
                        showIncorrectRkDialog = false
                    }
                )
            }

            val recoveryKeyLink = uiState.recoveryKeyLink
            if (recoveryKeyLink != null) {
                BasicInputDialog(
                    title = stringResource(id = R.string.title_dialog_insert_MK),
                    description = stringResource(id = R.string.text_dialog_insert_MK),
                    inputLabel = stringResource(id = R.string.edit_text_insert_mk),
                    inputValue = recoveryKeyInput,
                    onValueChange = {
                        recoveryKeyInput = it
                        if (recoveryKeyError != null) recoveryKeyError = null
                    },
                    errorText = recoveryKeyError,
                    positiveButtonText = stringResource(id = R.string.general_ok),
                    onPositiveButtonClicked = {
                        val value = recoveryKeyInput.trim()
                        if (value.isEmpty()) {
                            recoveryKeyError = context.getString(R.string.invalid_string)
                        } else {
                            keyboardController?.hide()
                            viewModel.checkRecoveryKey(recoveryKeyLink, value)
                            viewModel.onRecoveryKeyConsumed()
                        }
                    },
                    negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                    onNegativeButtonClicked = {
                        viewModel.onRecoveryKeyConsumed()
                    },
                )
            }
        }

        // Hide splash after UI is rendered, to prevent blinking
        LaunchedEffect(key1 = Unit) {
            delay(100)
            (activity as? LoginActivity)?.stopShowingSplashScreen()
        }
    }

    private fun openLoginIssueHelpdeskPage() {
        context.launchUrl(LOGIN_HELP_URL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupIntent()
    }

    /**
     * Gets data from the intent and performs the corresponding action if necessary.
     */
    @SuppressLint("NewApi")
    private fun setupIntent() = (requireActivity() as LoginActivity).intent?.let { intent ->
        intentAction = intent.action

        intentAction?.let { action ->
            Timber.d("action is: %s", action)
            when (action) {
                Constants.ACTION_CONFIRM -> {
                    handleConfirmationIntent(intent)
                    return
                }

                Constants.ACTION_RESET_PASS -> {
                    val link = intent.dataString
                    val isLoggedIn = intent.getBooleanExtra(LoginActivity.EXTRA_IS_LOGGED_IN, false)
                    if (link != null && !isLoggedIn) {
                        Timber.d("Link to resetPass: %s", link)
                        viewModel.onRequestRecoveryKey(link)
                        viewModel.intentSet()
                    }
                    return
                }

                Constants.ACTION_PASS_CHANGED -> {
                    when (intent.getIntExtra(Constants.RESULT, MegaError.API_OK)) {
                        MegaError.API_OK -> viewModel.setSnackbarMessageId(R.string.pass_changed_alert)
                    }
                    viewModel.intentSet()
                    return
                }

                Constants.ACTION_SHOW_WARNING_ACCOUNT_BLOCKED -> {
                    val accountBlockedString =
                        intent.getStringExtra(Constants.ACCOUNT_BLOCKED_STRING)
                    val accountBlockedType: AccountBlockedType? =
                        intent.serializable(Constants.ACCOUNT_BLOCKED_TYPE)

                    if (accountBlockedString != null && accountBlockedType != null && !TextUtil.isTextEmpty(
                            accountBlockedString
                        )
                    ) {
                        viewModel.triggerAccountBlockedEvent(
                            AccountBlockedDetail(
                                accountBlockedType,
                                accountBlockedString
                            )
                        )
                    }
                }

                ACTION_FORCE_RELOAD_ACCOUNT -> {
                    viewModel.setForceReloadAccountAsPendingAction()
                    return
                }
            }
        } ?: Timber.w("ACTION NULL")
    } ?: Timber.w("No INTENT")

    private fun finishSetupIntent(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            if (uiState.isAlreadyLoggedIn && !LoginActivity.isBackFromLoginPage) {
                Timber.d("Credentials NOT null")

                intentAction?.let { action ->
                    when (action) {
                        Constants.ACTION_REFRESH -> {
                            viewModel.fetchNodes(true)
                            return@apply
                        }

                        Constants.ACTION_REFRESH_API_SERVER -> {
                            intentParentHandle = getLongExtra("PARENT_HANDLE", -1)
                            startFastLogin()
                            return@apply
                        }

                        Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                            startFastLogin()
                            return@apply
                        }

                        else -> {
                            Timber.d("intent received $action")
                            when (action) {
                                Constants.ACTION_LOCATE_DOWNLOADED_FILE -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_SHOW_WARNING -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_EXPLORE_ZIP -> {
                                    intentExtras = extras
                                }

                                Constants.ACTION_OPEN_MEGA_FOLDER_LINK,
                                Constants.ACTION_IMPORT_LINK_FETCH_NODES,
                                Constants.ACTION_CHANGE_MAIL,
                                Constants.ACTION_CANCEL_ACCOUNT,
                                Constants.ACTION_OPEN_HANDLE_NODE,
                                Constants.ACTION_OPEN_CHAT_LINK,
                                Constants.ACTION_JOIN_OPEN_CHAT_LINK,
                                Constants.ACTION_RESET_PASS,
                                    -> {
                                    intentDataString = dataString
                                }

                                Constants.ACTION_FILE_PROVIDER -> {
                                    intentData = data
                                    intentExtras = extras
                                    intentDataString = null
                                }

                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL,
                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL,
                                    -> {
                                    intentData = data
                                }
                            }

                            if (uiState.rootNodesExists) {
                                var newIntent =
                                    Intent(requireContext(), ManagerActivity::class.java)

                                when (action) {
                                    Constants.ACTION_FILE_PROVIDER -> {
                                        newIntent =
                                            Intent(
                                                requireContext(),
                                                FileProviderActivity::class.java
                                            )
                                        intentExtras?.let { newIntent.putExtras(it) }
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                        newIntent = getFileLinkIntent()
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                        newIntent = getFolderLinkIntent()
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                        newIntent.data = intentData
                                    }

                                    Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                        newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        intentAction = Constants.ACTION_OPEN_CONTACTS_SECTION
                                        if (newIntent.getLongExtra(
                                                Constants.CONTACT_HANDLE,
                                                -1
                                            ) != -1L
                                        ) {
                                            newIntent.putExtra(
                                                Constants.CONTACT_HANDLE,
                                                newIntent.getLongExtra(Constants.CONTACT_HANDLE, -1)
                                            )
                                        }
                                    }
                                }

                                newIntent.action = intentAction

                                intentDataString?.let { newIntent.data = Uri.parse(it) }
                                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                                startActivity(newIntent)
                                requireActivity().finish()
                            } else {
                                startFastLogin()
                            }

                            return@apply
                        }
                    }
                }

                if (uiState.rootNodesExists && uiState.fetchNodesUpdate == null && !isIsHeartBeatAlive) {
                    Timber.d("rootNode != null")

                    var newIntent = Intent(requireContext(), ManagerActivity::class.java)

                    intentAction?.let { action ->
                        when (action) {
                            Constants.ACTION_FILE_PROVIDER -> {
                                newIntent =
                                    Intent(requireContext(), FileProviderActivity::class.java)
                                intentExtras?.let { newIntent.putExtras(it) }
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                newIntent = getFileLinkIntent()
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                newIntent = getFolderLinkIntent()
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                newIntent.data = intentData
                            }

                            Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                                if (getLongExtra(Constants.CONTACT_HANDLE, -1) != -1L) {
                                    newIntent.putExtra(
                                        Constants.CONTACT_HANDLE,
                                        getLongExtra(Constants.CONTACT_HANDLE, -1)
                                    )
                                }
                            }
                        }

                        newIntent.action = action
                        intentDataString?.let { newIntent.data = Uri.parse(it) }
                    }

                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                    startActivity(newIntent)
                    (requireActivity() as LoginActivity).finish()
                } else {
                    Timber.d("rootNode is null or heart beat is alive -> do fast login")
                    setHeartBeatAlive(false)
                    startFastLogin()
                }

                return@apply
            }

            Timber.d("Credentials IS NULL")
            Timber.d("INTENT NOT NULL")

            intentAction?.let { action ->
                Timber.d("ACTION NOT NULL")
                val newIntent: Intent
                when (action) {
                    Constants.ACTION_FILE_PROVIDER -> {
                        newIntent = Intent(requireContext(), FileProviderActivity::class.java)
                        intentExtras?.let { newIntent.putExtras(it) }
                        newIntent.data = intentData
                        newIntent.action = action
                    }

                    Constants.ACTION_FILE_EXPLORER_UPLOAD -> {
                        viewModel.setSnackbarMessageId(R.string.login_before_share)
                    }

                    Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                        intentDataString = dataString
                    }
                }
            }
        }

        viewModel.intentSet()
    }


    /**
     * Handles intent from confirmation email.
     *
     * @param intent Intent.
     */
    private fun handleConfirmationIntent(intent: Intent) {
        if (!viewModel.isConnected) {
            viewModel.setSnackbarMessageId(R.string.error_server_connection_problem)
            return
        }

        Timber.d("querySignupLink")
        intent.getStringExtra(Constants.EXTRA_CONFIRMATION)?.let { viewModel.checkSignupLink(it) }
    }

    private fun startFastLogin() {
        Timber.d("startFastLogin")
        viewModel.fastLogin(requireActivity().intent?.action == Constants.ACTION_REFRESH_API_SERVER)
    }

    /**
     * Checks pending actions and setups the final intent for launch before finish.
     */
    private fun readyToFinish(uiState: LoginState) {
        (requireActivity() as LoginActivity).intent?.apply {
            Timber.d("Intent not null")

            @Suppress("UNCHECKED_CAST")
            intentShareInfo = getBooleanExtra(FileExplorerActivity.EXTRA_FROM_SHARE, false)

            when {
                intentShareInfo -> {
                    Timber.d("Intent to share")
                    toSharePage()
                    return
                }

                Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == type -> {
                    Timber.d("Intent to FileExplorerActivity")
                    startActivity(
                        Intent(
                            requireContext(),
                            FileExplorerActivity::class.java
                        ).putExtra(
                            Intent.EXTRA_TEXT,
                            getStringExtra(Intent.EXTRA_TEXT)
                        )
                            .putExtra(
                                Intent.EXTRA_SUBJECT,
                                getStringExtra(Intent.EXTRA_SUBJECT)
                            )
                            .putExtra(
                                Intent.EXTRA_EMAIL,
                                getStringExtra(Intent.EXTRA_EMAIL)
                            )
                            .setAction(Intent.ACTION_SEND)
                            .setType(Constants.TYPE_TEXT_PLAIN)
                    )
                    requireActivity().finish()
                    return
                }

                Constants.ACTION_REFRESH == action && activity != null -> {
                    Timber.d("Intent to refresh")
                    requireActivity().apply {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    return
                }
            }
        }


        val loginActivity = requireActivity() as LoginActivity
        val isLoggedInToConfirmedAccount =
            !loginActivity.intent.getStringExtra(Constants.EXTRA_CONFIRMATION).isNullOrEmpty()
                    && uiState.isAccountConfirmed
                    && uiState.accountSession?.email == uiState.temporalEmail
        if (!(isLoggedInToConfirmedAccount && uiState.shouldShowUpgradeAccount)) {
            if (getChatManagement().isPendingJoinLink()) {
                LoginActivity.isBackFromLoginPage = false
                getChatManagement().pendingJoinLink = null
            }
            Timber.d("confirmLink==null")
            Timber.d("OK fetch nodes")

            if (intentAction != null && intentDataString != null) {
                Timber.d("Intent action: $intentAction")

                when (intentAction) {
                    Constants.ACTION_CHANGE_MAIL -> {
                        Timber.d("Action change mail after fetch nodes")
                        val changeMailIntent = Intent(requireContext(), ManagerActivity::class.java)
                        changeMailIntent.action = Constants.ACTION_CHANGE_MAIL
                        changeMailIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(changeMailIntent)
                        loginActivity.finish()
                    }

                    Constants.ACTION_RESET_PASS -> {
                        Timber.d("Action reset pass after fetch nodes")
                        val resetPassIntent = Intent(requireContext(), ManagerActivity::class.java)
                        resetPassIntent.action = Constants.ACTION_RESET_PASS
                        resetPassIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(resetPassIntent)
                        loginActivity.finish()
                    }

                    Constants.ACTION_CANCEL_ACCOUNT -> {
                        Timber.d("Action cancel Account after fetch nodes")
                        val cancelAccountIntent =
                            Intent(requireContext(), ManagerActivity::class.java)
                        cancelAccountIntent.action = Constants.ACTION_CANCEL_ACCOUNT
                        cancelAccountIntent.data = Uri.parse(intentDataString)
                        loginActivity.startActivity(cancelAccountIntent)
                        loginActivity.finish()
                    }
                }
            }
            if (!uiState.pressedBackWhileLogin) {
                Timber.d("NOT backWhileLogin")

                if (intentParentHandle != -1L) {
                    Timber.d("Activity result OK")
                    val intent = Intent()
                    intent.putExtra("PARENT_HANDLE", intentParentHandle)
                    loginActivity.setResult(Activity.RESULT_OK, intent)
                    loginActivity.finish()
                } else {
                    lifecycleScope.launch {
                        var intent: Intent?
                        val refreshActivityIntent =
                            requireActivity().intent.parcelable<Intent>(LAUNCH_INTENT)
                        if (uiState.isAlreadyLoggedIn) {
                            Timber.d("isAlreadyLoggedIn")
                            intent = Intent(requireContext(), ManagerActivity::class.java)
                            setStartScreenTimeStamp(requireContext())
                            when (intentAction) {
                                Constants.ACTION_EXPORT_MASTER_KEY -> {
                                    Timber.d("ACTION_EXPORT_MK")
                                    intent.action = Constants.ACTION_EXPORT_MASTER_KEY
                                }

                                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                    if (intentDataString != null) {
                                        intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                                        intent.data = Uri.parse(intentDataString)
                                    }
                                }

                                else -> intent =
                                    refreshActivityIntent ?: handleLinkNavigation(loginActivity)
                            }
                            if (uiState.isFirstTime) {
                                Timber.d("First time")
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                            }
                        } else {
                            var initialCam = false
                            if (uiState.hasPreferences) {
                                if (!uiState.hasCUSetting) {
                                    with(requireActivity()) {
                                        setStartScreenTimeStamp(this)

                                        Timber.d("First login")
                                        startActivity(
                                            Intent(
                                                this,
                                                ManagerActivity::class.java
                                            ).apply {
                                                putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                                            })

                                        finish()
                                    }
                                    initialCam = true
                                }
                            } else {
                                intent = Intent(requireContext(), ManagerActivity::class.java)
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                                initialCam = true
                                setStartScreenTimeStamp(requireContext())
                            }
                            if (!initialCam) {
                                Timber.d("NOT initialCam")
                                intent = handleLinkNavigation(loginActivity)
                            } else {
                                Timber.d("initialCam YES")
                                intent = Intent(requireContext(), ManagerActivity::class.java)
                                Timber.d("The action is: %s", intentAction)
                                intent.action = intentAction
                                intentDataString?.let { intent.data = Uri.parse(it) }
                            }
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        if (intentAction == Constants.ACTION_REFRESH_API_SERVER
                            || intentAction == Constants.ACTION_REFRESH_AFTER_BLOCKED
                        ) {
                            intent.action = intentAction
                        }

                        if (viewModel.getStorageState() === StorageState.PayWall) {
                            Timber.d("show Paywall warning")
                            showOverDiskQuotaPaywallWarning(activity, true)
                        } else {
                            Timber.d("First launch")
                            val shouldShowNotificationPermission =
                                viewModel.shouldShowNotificationPermission()
                            intent.apply {
                                putExtra(
                                    IntentConstants.EXTRA_FIRST_LAUNCH,
                                    uiState.isFirstTimeLaunch
                                )
                                if (shouldShowNotificationPermission) {
                                    Timber.d("LoginFragment::shouldShowNotificationPermission")
                                    putExtra(
                                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                                        true
                                    )
                                    putExtra(
                                        IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION,
                                        true
                                    )
                                }
                            }

                            // we show upgrade account for all accounts that are free and logged in for the first time
                            if (uiState.shouldShowUpgradeAccount) {
                                loginActivity.startActivity(
                                    intent.setClass(
                                        requireContext(),
                                        ChooseAccountActivity::class.java
                                    ).apply {
                                        putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                                        putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, false)
                                    }
                                )
                            } else {
                                loginActivity.startActivity(intent)
                            }
                        }
                        Timber.d("LoginActivity finish")
                        loginActivity.finish()
                    }
                }
            }
        } else {
            Timber.d("Go to ChooseAccountFragment")
            viewModel.updateIsAccountConfirmed(false)
            if (getChatManagement().isPendingJoinLink()) {
                LoginActivity.isBackFromLoginPage = false
                val intent = Intent(requireContext(), ManagerActivity::class.java)
                intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                intent.data = Uri.parse(getChatManagement().pendingJoinLink)
                startActivity(intent)
                getChatManagement().pendingJoinLink = null
                loginActivity.finish()
            } else if (uiState.isAlreadyLoggedIn) {
                startActivity(Intent(loginActivity, ChooseAccountActivity::class.java))
                loginActivity.finish()
            }
        }
    }

    private fun handleLinkNavigation(loginActivity: LoginActivity): Intent {
        var intent = Intent(requireContext(), ManagerActivity::class.java)
        if (intentAction != null) {
            Timber.d("The action is: %s", intentAction)
            when (intentAction) {
                Constants.ACTION_FILE_PROVIDER -> {
                    intent = Intent(requireContext(), FileProviderActivity::class.java)
                    intentExtras?.let { intent.putExtras(it) }
                    intent.data = intentData
                }

                Constants.ACTION_LOCATE_DOWNLOADED_FILE -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_SHOW_WARNING -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_EXPLORE_ZIP -> {
                    intentExtras?.let { intent.putExtras(it) }
                }

                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                    intent = getFileLinkIntent()
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.data = intentData
                }

                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                    intent = getFolderLinkIntent()
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                    intent.data = intentData
                }

                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                    intent.putExtra(
                        Constants.CONTACT_HANDLE,
                        loginActivity.intent?.getLongExtra(Constants.CONTACT_HANDLE, -1),
                    )
                }
            }
            intent.action = intentAction
            intentDataString?.let { intent.data = Uri.parse(it) }
        } else {
            Timber.w("The intent action is NULL")
        }
        return intent
    }

    /**
     * Launches an intent to [FileExplorerActivity]
     */
    private fun toSharePage() = with(requireActivity()) {
        startActivity(
            this.intent.setClass(requireContext(), FileExplorerActivity::class.java)
        )
        finish()
    }

    private fun navigateToChangePassword(link: String, value: String) {
        val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
        intent.action = Constants.ACTION_RESET_PASS_FROM_LINK
        intent.data = link.toUri()
        intent.putExtra(IntentConstants.EXTRA_MASTER_KEY, value)
        startActivity(intent)
    }

    /**
     * Performs on back pressed.
     *
     * # Disable back press when:
     * - Refreshing
     * # Minimize app when:
     * - Login is in progress
     * - Nodes are being fetched
     * # Show logout confirmation when:
     * - 2FA is required
     */
    fun onBackPressed(uiState: LoginState) {
        Timber.d("onBackPressed")
        with(uiState) {
            when {
                Constants.ACTION_REFRESH == intentAction || Constants.ACTION_REFRESH_API_SERVER == intentAction ->
                    return

                is2FARequired || multiFactorAuthState != null -> {
                    viewModel.stopLogin()
                }

                loginMutex.isLocked || isLoginInProgress || isFastLoginInProgress || fetchNodesUpdate != null ->
                    activity?.moveTaskToBack(true)

                else -> {
                    LoginActivity.isBackFromLoginPage = true
                    (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Tour)
                }
            }
        }
    }

    private fun getFolderLinkIntent(): Intent {
        return Intent(requireContext(), FolderLinkComposeActivity::class.java)
    }

    private fun getFileLinkIntent(): Intent {
        return Intent(requireContext(), FileLinkComposeActivity::class.java)
    }

    companion object {
        private const val LOGIN_HELP_URL = "https://help.mega.io/accounts/login-issues"
    }
}

private fun onLostAuthenticationDevice(context: Context) {
    context.launchUrl(RECOVERY_URL)
}

private fun onForgotPassword(context: Context, typedEmail: String?) {
    Timber.d("Click on button_forgot_pass")
    context.launchUrl(
        if (typedEmail.isNullOrEmpty()) {
            RECOVERY_URL
        } else {
            RECOVERY_URL_EMAIL + Base64.encodeToString(typedEmail.toByteArray(), Base64.DEFAULT)
                .replace("\n", "")
        }
    )
}