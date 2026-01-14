package mega.privacy.android.app.presentation.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.LoginActivity.Companion.ACTION_REFRESH_AND_OPEN_SESSION_LINK
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.core.sharedcomponents.parcelable
import mega.privacy.android.core.sharedcomponents.serializable
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountActivity
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.megaNavigator
import nz.mega.sdk.MegaError
import timber.log.Timber

/**
 * Handles the login intent actions
 */
@Composable
fun LoginIntentActionHandler(viewModel: LoginViewModel, uiState: LoginState) {
    var intentExtras: Bundle? by remember { mutableStateOf(null) }
    var intentData: Uri? by remember { mutableStateOf(null) }
    var intentAction: String? by remember { mutableStateOf(null) }
    var intentDataString: String? by remember { mutableStateOf(null) }
    var intentParentHandle: Long by remember { mutableLongStateOf(-1L) }
    var intentShareInfo: Boolean by remember { mutableStateOf(false) }
    var isPendingToGetLinkWithSession: Boolean by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current ?: return

    val readyToFinish = remember {
        ReadyToFinish@{ loginUiState: LoginState ->
            activity.intent?.apply {
                Timber.d("Intent not null")

                intentShareInfo = getBooleanExtra(FileExplorerActivity.EXTRA_FROM_SHARE, false)

                when {
                    intentShareInfo -> {
                        Timber.d("Intent to share")
                        toSharePage(activity)
                        return@ReadyToFinish
                    }

                    Constants.ACTION_FILE_EXPLORER_UPLOAD == intentAction && Constants.TYPE_TEXT_PLAIN == type -> {
                        Timber.d("Intent to FileExplorerActivity")
                        activity.startActivity(
                            Intent(
                                activity,
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
                        activity.finish()
                        return@ReadyToFinish
                    }

                    Constants.ACTION_REFRESH == intentAction || LoginViewModel.ACTION_FORCE_RELOAD_ACCOUNT == intentAction -> {
                        Timber.d("Intent to refresh")
                        activity.apply {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        return@ReadyToFinish
                    }

                    ACTION_REFRESH_AND_OPEN_SESSION_LINK == intentAction -> {
                        dataString?.let { viewModel.getLinkWithSession(it) }
                    }
                }
            }

            val isLoggedInToConfirmedAccount =
                !activity.intent.getStringExtra(Constants.EXTRA_CONFIRMATION).isNullOrEmpty()
                        && loginUiState.isAccountConfirmed
                        && loginUiState.accountSession?.email == loginUiState.temporalEmail
            if (!(isLoggedInToConfirmedAccount && loginUiState.shouldShowUpgradeAccount)) {
                if (MegaApplication.getChatManagement().isPendingJoinLink()) {
                    LoginActivity.isBackFromLoginPage = false
                    MegaApplication.getChatManagement().pendingJoinLink = null
                }
                Timber.d("confirmLink==null")
                Timber.d("OK fetch nodes")

                if (intentAction != null && intentDataString != null) {
                    Timber.d("Intent action: $intentAction")

                    when (intentAction) {
                        Constants.ACTION_CHANGE_MAIL -> {
                            Timber.d("Action change mail after fetch nodes")
                            activity.megaNavigator.openManagerActivity(
                                context = activity,
                                action = Constants.ACTION_CHANGE_MAIL,
                                data = intentDataString?.toUri(),
                                singleActivityDestination = null,
                            )
                            activity.finish()
                        }

                        Constants.ACTION_RESET_PASS -> {
                            Timber.d("Action reset pass after fetch nodes")
                            activity.megaNavigator.openManagerActivity(
                                context = activity,
                                action = Constants.ACTION_RESET_PASS,
                                data = intentDataString?.toUri(),
                                singleActivityDestination = null,
                            )
                            activity.finish()
                        }

                        Constants.ACTION_CANCEL_ACCOUNT -> {
                            Timber.d("Action cancel Account after fetch nodes")
                            activity.megaNavigator.openManagerActivity(
                                context = activity,
                                action = Constants.ACTION_CANCEL_ACCOUNT,
                                data = intentDataString?.toUri(),
                                singleActivityDestination = null,
                            )
                            activity.finish()
                        }
                    }
                }
                if (!loginUiState.pressedBackWhileLogin) {
                    Timber.d("NOT backWhileLogin")

                    if (intentParentHandle != -1L) {
                        Timber.d("Activity result OK")
                        val intent = Intent()
                        intent.putExtra("PARENT_HANDLE", intentParentHandle)
                        activity.setResult(Activity.RESULT_OK, intent)
                    } else {
                        var intent: Intent? = null
                        var action: String? = null
                        var data: Uri? = null
                        var flags: Int? = null
                        var bundle = Bundle()

                        if (loginUiState.isAlreadyLoggedIn) {
                            Timber.d("isAlreadyLoggedIn")
                            viewModel.setStartScreenTimeStamp()
                            when (intentAction) {
                                Constants.ACTION_EXPORT_MASTER_KEY -> {
                                    Timber.d("ACTION_EXPORT_MK")
                                    action = Constants.ACTION_EXPORT_MASTER_KEY
                                }

                                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                    if (intentDataString != null) {
                                        action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                                        data = intentDataString?.toUri()
                                    }
                                }

                                else -> intent =
                                    activity.intent.parcelable<Intent>(Constants.LAUNCH_INTENT)
                                        ?: handleLinkNavigation(
                                            activity = activity,
                                            intentAction = intentAction,
                                            intentExtras = intentExtras,
                                            intentData = intentData,
                                            intentDataString = intentDataString
                                        ).let { intentInfo ->
                                            action = intentInfo.action
                                            data = intentInfo.data
                                            intentInfo.bundle?.let { bundle = it }

                                            intentInfo.intent
                                        }
                            }

                            if (loginUiState.isFirstTime) {
                                Timber.d("First time")
                                intent?.putExtra(ExtraConstant.EXTRA_FIRST_LOGIN, true)
                                    ?: bundle.apply {
                                        putBoolean(ExtraConstant.EXTRA_FIRST_LOGIN, true)
                                    }
                            }
                        } else {
                            var initialCam = false
                            if (loginUiState.hasPreferences) {
                                if (!loginUiState.hasCUSetting) {
                                    with(activity) {
                                        viewModel.setStartScreenTimeStamp()

                                        Timber.d("First login")
                                        activity.megaNavigator.openManagerActivity(
                                            context = activity,
                                            bundle = Bundle().apply {
                                                putBoolean(
                                                    ExtraConstant.EXTRA_FIRST_LOGIN,
                                                    true,
                                                )
                                            },
                                            singleActivityDestination = null,
                                        )

                                        finish()
                                    }
                                }
                            } else {
                                bundle.apply {
                                    putBoolean(ExtraConstant.EXTRA_FIRST_LOGIN, true)
                                }
                                initialCam = true
                                viewModel.setStartScreenTimeStamp()
                            }
                            if (!initialCam) {
                                Timber.d("NOT initialCam")
                                intent = handleLinkNavigation(
                                    activity = activity,
                                    intentAction = intentAction,
                                    intentExtras = intentExtras,
                                    intentData = intentData,
                                    intentDataString = intentDataString
                                ).let { intentInfo ->
                                    action = intentInfo.action
                                    data = intentInfo.data
                                    intentInfo.bundle?.let { bundle = it }

                                    intentInfo.intent
                                }
                            } else {
                                Timber.d("initialCam YES")
                                Timber.d("The action is: %s", intentAction)
                                action = intentAction
                                intentDataString?.let { data = it.toUri() }
                            }
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        if (intentAction == Constants.ACTION_REFRESH_API_SERVER
                            || intentAction == Constants.ACTION_REFRESH_AFTER_BLOCKED
                        ) {
                            action = intentAction
                        }

                        if (viewModel.getStorageState() === StorageState.PayWall) {
                            Timber.d("show Paywall warning")
                            AlertsAndWarnings.showOverDiskQuotaPaywallWarning(activity, true)
                        } else {
                            Timber.d("First launch")
                            intent?.let {
                                it.putExtra(
                                    IntentConstants.EXTRA_FIRST_LAUNCH,
                                    loginUiState.isFirstTimeLaunch
                                )
                                if (uiState.shouldShowNotificationPermission) {
                                    Timber.d("LoginFragment::shouldShowNotificationPermission")
                                    it.putExtra(
                                        IntentConstants.EXTRA_ASK_PERMISSIONS,
                                        true
                                    )
                                    it.putExtra(
                                        IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION,
                                        true
                                    )
                                }
                            } ?: bundle.apply {
                                putBoolean(
                                    IntentConstants.EXTRA_FIRST_LAUNCH,
                                    loginUiState.isFirstTimeLaunch
                                )
                                if (uiState.shouldShowNotificationPermission) {
                                    Timber.d("LoginFragment::shouldShowNotificationPermission")
                                    putBoolean(IntentConstants.EXTRA_ASK_PERMISSIONS, true)
                                    putBoolean(
                                        IntentConstants.EXTRA_SHOW_NOTIFICATION_PERMISSION,
                                        true
                                    )
                                }
                            }

                            // we show upgrade account for all accounts that are free and logged in for the first time
                            if (loginUiState.shouldShowUpgradeAccount) {
                                activity.startActivity(
                                    (intent?.setClass(
                                        activity,
                                        ChooseAccountActivity::class.java
                                    ) ?: Intent(activity, ChooseAccountActivity::class.java))
                                        .apply {
                                            this.action = action
                                            this.data = data
                                            flags?.let { this.flags = it }
                                            putExtras(bundle)
                                            putExtra(ExtraConstant.EXTRA_NEW_ACCOUNT, false)
                                            putExtra(ExtraConstant.NEW_CREATION_ACCOUNT, false)
                                        }
                                )
                            } else {
                                intent?.let { activity.startActivity(intent) }
                                    ?: activity.megaNavigator.openManagerActivity(
                                        context = activity,
                                        action = action,
                                        data = data,
                                        bundle = bundle,
                                        flags = flags,
                                        singleActivityDestination = null,
                                    )
                            }
                        }
                    }
                    if (!isPendingToGetLinkWithSession) {
                        Timber.d("LoginActivity finish")
                        activity.finish()
                    }
                }
            } else {
                Timber.d("Go to ChooseAccountActivity")
                viewModel.updateIsAccountConfirmed(false)
                if (MegaApplication.getChatManagement().isPendingJoinLink()) {
                    LoginActivity.isBackFromLoginPage = false
                    activity.megaNavigator.openManagerActivity(
                        context = activity,
                        action = Constants.ACTION_JOIN_OPEN_CHAT_LINK,
                        data = MegaApplication.getChatManagement().pendingJoinLink?.toUri(),
                        singleActivityDestination = null,
                    )
                    MegaApplication.getChatManagement().pendingJoinLink = null
                    activity.finish()
                } else if (loginUiState.isAlreadyLoggedIn) {
                    activity.startActivity(Intent(activity, ChooseAccountActivity::class.java))
                    activity.finish()
                }
            }
        }
    }

    val finishSetupIntent = remember {
        { loginUiState: LoginState ->
            activity.intent?.apply {
                if (loginUiState.isAlreadyLoggedIn && !LoginActivity.isBackFromLoginPage) {
                    Timber.d("Credentials NOT null")

                    intentAction?.let {
                        when (intentAction) {
                            Constants.ACTION_REFRESH -> {
                                viewModel.fetchNodes(true)
                                return@apply
                            }

                            Constants.ACTION_REFRESH_API_SERVER -> {
                                intentParentHandle = getLongExtra("PARENT_HANDLE", -1)
                                viewModel.fastLogin(activity.intent?.action == Constants.ACTION_REFRESH_API_SERVER)
                                return@apply
                            }

                            Constants.ACTION_REFRESH_AFTER_BLOCKED -> {
                                viewModel.fastLogin(activity.intent?.action == Constants.ACTION_REFRESH_API_SERVER)
                                return@apply
                            }

                            else -> {
                                Timber.d("intent received $intentAction")
                                when (intentAction) {
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

                                    Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL,
                                    Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL,
                                        -> {
                                        intentData = data
                                    }
                                }

                                if (loginUiState.rootNodesExists && intentAction != LoginViewModel.ACTION_FORCE_RELOAD_ACCOUNT) {
                                    var newIntent: Intent? = null
                                    var flags: Int? = null
                                    val bundle = Bundle()

                                    when (intentAction) {
                                        Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                            newIntent = getFileLinkIntent(activity)
                                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                            newIntent.data = intentData
                                        }

                                        Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                            newIntent = getFolderLinkIntent(activity)
                                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                            newIntent.data = intentData
                                        }

                                        Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            intentAction = Constants.ACTION_OPEN_CONTACTS_SECTION

                                            getLongExtra(
                                                Constants.CONTACT_HANDLE,
                                                -1
                                            ).takeIf { it != -1L }?.let { contactHandle ->
                                                bundle.apply {
                                                    putLong(Constants.CONTACT_HANDLE, contactHandle)
                                                }
                                            }
                                        }
                                    }

                                    newIntent?.action = intentAction

                                    intentDataString?.let { newIntent?.data = it.toUri() }
                                    newIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    flags = if (flags == null) {
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    } else {
                                        flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }

                                    if (newIntent != null) {
                                        activity.startActivity(newIntent)
                                    } else {
                                        activity.megaNavigator.openManagerActivity(
                                            context = activity,
                                            action = intentAction,
                                            data = intentDataString?.toUri(),
                                            bundle = bundle,
                                            flags = flags,
                                            singleActivityDestination = null,
                                        )
                                    }

                                    activity.finish()
                                } else {
                                    if (intentAction == ACTION_REFRESH_AND_OPEN_SESSION_LINK) {
                                        isPendingToGetLinkWithSession = true
                                        viewModel.setPendingToGetLinkWithSession()
                                    }

                                    viewModel.fastLogin(activity.intent?.action == Constants.ACTION_REFRESH_API_SERVER)
                                }

                                return@apply
                            }
                        }
                    }

                    if (loginUiState.rootNodesExists && loginUiState.fetchNodesUpdate == null && !MegaApplication.isIsHeartBeatAlive) {
                        Timber.d("rootNode != null")

                        var newIntent: Intent? = null
                        var flags: Int? = null
                        val bundle = Bundle()

                        intentAction?.let {
                            when (intentAction) {
                                Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                                    newIntent = getFileLinkIntent(activity)
                                    newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    intentAction = Constants.ACTION_OPEN_MEGA_LINK
                                    newIntent.data = intentData
                                }

                                Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                                    newIntent = getFolderLinkIntent(activity)
                                    newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    intentAction = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                                    newIntent.data = intentData
                                }

                                Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                                    getLongExtra(
                                        Constants.CONTACT_HANDLE,
                                        -1
                                    ).takeIf { it != -1L }?.let { contactHandle ->
                                        bundle.apply {
                                            putLong(Constants.CONTACT_HANDLE, contactHandle)
                                        }
                                    }
                                }
                            }

                            newIntent?.action = intentAction
                            intentDataString?.let { newIntent?.data = it.toUri() }
                        }

                        newIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        flags = if (flags == null) {
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                        } else {
                            flags or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        if (newIntent != null) {
                            activity.startActivity(newIntent)
                        } else {
                            activity.megaNavigator.openManagerActivity(
                                context = activity,
                                action = intentAction,
                                data = intentDataString?.toUri(),
                                bundle = bundle,
                                flags = flags,
                                singleActivityDestination = null,
                            )
                        }

                        activity.finish()
                    } else {
                        Timber.d("rootNode is null or heart beat is alive -> do fast login")
                        MegaApplication.setHeartBeatAlive(false)
                        viewModel.fastLogin(activity.intent?.action == Constants.ACTION_REFRESH_API_SERVER)
                    }

                    return@apply
                }

                Timber.d("Credentials IS NULL")
                Timber.d("INTENT NOT NULL")

                intentAction?.let { action ->
                    Timber.d("ACTION NOT NULL")
                    when (action) {
                        Constants.ACTION_FILE_EXPLORER_UPLOAD -> {
                            viewModel.setSnackbarMessageId(R.string.login_before_share)
                        }
                    }
                }
            }

            viewModel.intentSet()
        }
    }

    LaunchedEffect(Unit) {
        activity.intent?.let { intent ->
            intentAction = intent.action

            intentAction?.let { action ->
                Timber.d("action is: %s", action)
                when (action) {
                    //Already managed in LoginNavigation.checkActions for deep links and new navigation.
                    Constants.ACTION_CONFIRM -> {
                        Timber.d("querySignupLink")
                        intent.getStringExtra(Constants.EXTRA_CONFIRMATION)
                            ?.let { viewModel.checkSignupLink(it) }
                        return@LaunchedEffect
                    }

                    //Already managed in LoginNavigation.checkActions for deep links and new navigation.
                    Constants.ACTION_RESET_PASS -> {
                        val link = intent.dataString
                        val isLoggedIn =
                            intent.getBooleanExtra(LoginActivity.EXTRA_IS_LOGGED_IN, false)
                        if (link != null && !isLoggedIn) {
                            Timber.d("Link to resetPass: %s", link)
                            viewModel.onRequestRecoveryKey(link)
                            viewModel.intentSet()
                        }
                        return@LaunchedEffect
                    }

                    Constants.ACTION_PASS_CHANGED -> {
                        when (intent.getIntExtra(Constants.RESULT, MegaError.API_OK)) {
                            MegaError.API_OK -> viewModel.setSnackbarMessageId(R.string.pass_changed_alert)
                        }
                        viewModel.intentSet()
                        return@LaunchedEffect
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
                                AccountBlockedEvent(
                                    handle = -1L,
                                    type = accountBlockedType,
                                    text = accountBlockedString
                                )
                            )
                        }
                    }

                    Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                        intentDataString = intent.dataString
                    }

                    LoginViewModel.ACTION_FORCE_RELOAD_ACCOUNT -> {
                        viewModel.setForceReloadAccountAsPendingAction()
                        return@LaunchedEffect
                    }
                }
            } ?: Timber.w("ACTION NULL")
        } ?: Timber.w("No INTENT")
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

                LoginIntentState.AlreadySet -> {
                    activity.intent.action = null
                }

                else -> {
                    /* Nothing to update */
                    Timber.d("Intent state: $this")
                }
            }
        }
    }
}

private fun handleLinkNavigation(
    activity: Activity,
    intentAction: String?,
    intentExtras: Bundle?,
    intentData: Uri?,
    intentDataString: String?,
): IntentInfo {
    var intent: Intent? = null
    var bundle = Bundle()

    if (intentAction != null) {
        Timber.d("The action is: %s", intentAction)
        when (intentAction) {
            Constants.ACTION_LOCATE_DOWNLOADED_FILE,
            Constants.ACTION_SHOW_WARNING,
            Constants.ACTION_EXPLORE_ZIP,
                -> {
                intentExtras?.let { bundle = intentExtras }
            }

            Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL -> {
                intent = getFileLinkIntent(activity)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.data = intentData
            }

            Constants.ACTION_OPEN_FOLDER_LINK_ROOTNODES_NULL -> {
                intent = getFolderLinkIntent(activity)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                intent.data = intentData
            }

            Constants.ACTION_OPEN_CONTACTS_SECTION -> {
                bundle.putLong(
                    Constants.CONTACT_HANDLE,
                    activity.intent?.getLongExtra(Constants.CONTACT_HANDLE, -1) ?: -1,
                )
            }
        }

        intent?.action = intentAction
        intentDataString?.let { intent?.data = it.toUri() }
    } else {
        Timber.w("The intent action is NULL")
    }
    return if (intent != null) {
        IntentInfo(intent = intent)
    } else {
        IntentInfo(
            bundle = bundle,
            action = intentAction,
            data = intentDataString?.toUri()
        )
    }
}

private data class IntentInfo(
    val intent: Intent? = null,
    val bundle: Bundle? = null,
    val action: String? = null,
    val data: Uri? = null,
)

private fun toSharePage(activity: Activity) = with(activity) {
    startActivity(
        this.intent.setClass(activity, FileExplorerActivity::class.java)
    )
    finish()
}

private fun getFolderLinkIntent(context: Context): Intent {
    return Intent(context, FolderLinkComposeActivity::class.java)
}

private fun getFileLinkIntent(context: Context): Intent {
    return Intent(context, FileLinkComposeActivity::class.java)
}