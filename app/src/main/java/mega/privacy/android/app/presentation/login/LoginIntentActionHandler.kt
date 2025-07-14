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
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.extensions.serializable
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.settings.startscreen.util.StartScreenUtil
import mega.privacy.android.app.providers.FileProviderActivity
import mega.privacy.android.app.upgradeAccount.ChooseAccountActivity
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
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

                    Constants.ACTION_FILE_EXPLORER_UPLOAD == action && Constants.TYPE_TEXT_PLAIN == type -> {
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

                    Constants.ACTION_REFRESH == action -> {
                        Timber.d("Intent to refresh")
                        activity.apply {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                        return@ReadyToFinish
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
                            val changeMailIntent = Intent(activity, ManagerActivity::class.java)
                            changeMailIntent.action = Constants.ACTION_CHANGE_MAIL
                            changeMailIntent.data = intentDataString?.toUri()
                            activity.startActivity(changeMailIntent)
                            activity.finish()
                        }

                        Constants.ACTION_RESET_PASS -> {
                            Timber.d("Action reset pass after fetch nodes")
                            val resetPassIntent = Intent(activity, ManagerActivity::class.java)
                            resetPassIntent.action = Constants.ACTION_RESET_PASS
                            resetPassIntent.data = intentDataString?.toUri()
                            activity.startActivity(resetPassIntent)
                            activity.finish()
                        }

                        Constants.ACTION_CANCEL_ACCOUNT -> {
                            Timber.d("Action cancel Account after fetch nodes")
                            val cancelAccountIntent =
                                Intent(activity, ManagerActivity::class.java)
                            cancelAccountIntent.action = Constants.ACTION_CANCEL_ACCOUNT
                            cancelAccountIntent.data = intentDataString?.toUri()
                            activity.startActivity(cancelAccountIntent)
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
                        activity.finish()
                    } else {
                        var intent: Intent
                        val refreshActivityIntent =
                            activity.intent.parcelable<Intent>(Constants.LAUNCH_INTENT)
                        if (loginUiState.isAlreadyLoggedIn) {
                            Timber.d("isAlreadyLoggedIn")
                            intent = Intent(activity, ManagerActivity::class.java)
                            StartScreenUtil.setStartScreenTimeStamp(activity)
                            when (intentAction) {
                                Constants.ACTION_EXPORT_MASTER_KEY -> {
                                    Timber.d("ACTION_EXPORT_MK")
                                    intent.action = Constants.ACTION_EXPORT_MASTER_KEY
                                }

                                Constants.ACTION_JOIN_OPEN_CHAT_LINK -> {
                                    if (intentDataString != null) {
                                        intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                                        intent.data = intentDataString?.toUri()
                                    }
                                }

                                else -> intent =
                                    refreshActivityIntent ?: handleLinkNavigation(
                                        activity = activity,
                                        intentAction = intentAction,
                                        intentExtras = intentExtras,
                                        intentData = intentData,
                                        intentDataString = intentDataString
                                    )
                            }
                            if (loginUiState.isFirstTime) {
                                Timber.d("First time")
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                            }
                        } else {
                            var initialCam = false
                            if (loginUiState.hasPreferences) {
                                if (!loginUiState.hasCUSetting) {
                                    with(activity) {
                                        StartScreenUtil.setStartScreenTimeStamp(
                                            this
                                        )

                                        Timber.d("First login")
                                        startActivity(
                                            Intent(
                                                this,
                                                ManagerActivity::class.java
                                            ).apply {
                                                putExtra(
                                                    IntentConstants.EXTRA_FIRST_LOGIN,
                                                    true
                                                )
                                            })

                                        finish()
                                    }
                                    initialCam = true
                                }
                            } else {
                                intent = Intent(activity, ManagerActivity::class.java)
                                intent.putExtra(IntentConstants.EXTRA_FIRST_LOGIN, true)
                                initialCam = true
                                StartScreenUtil.setStartScreenTimeStamp(activity)
                            }
                            if (!initialCam) {
                                Timber.d("NOT initialCam")
                                intent = handleLinkNavigation(
                                    activity = activity,
                                    intentAction = intentAction,
                                    intentExtras = intentExtras,
                                    intentData = intentData,
                                    intentDataString = intentDataString
                                )
                            } else {
                                Timber.d("initialCam YES")
                                intent = Intent(activity, ManagerActivity::class.java)
                                Timber.d("The action is: %s", intentAction)
                                intent.action = intentAction
                                intentDataString?.let { intent.data = it.toUri() }
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
                            AlertsAndWarnings.showOverDiskQuotaPaywallWarning(activity, true)
                        } else {
                            Timber.d("First launch")
                            intent.apply {
                                putExtra(
                                    IntentConstants.EXTRA_FIRST_LAUNCH,
                                    loginUiState.isFirstTimeLaunch
                                )
                                if (uiState.shouldShowNotificationPermission) {
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
                            if (loginUiState.shouldShowUpgradeAccount) {
                                activity.startActivity(
                                    intent.setClass(
                                        activity,
                                        ChooseAccountActivity::class.java
                                    ).apply {
                                        putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, false)
                                        putExtra(ManagerActivity.NEW_CREATION_ACCOUNT, false)
                                    }
                                )
                            } else {
                                activity.startActivity(intent)
                            }
                        }
                        Timber.d("LoginActivity finish")
                        activity.finish()
                    }
                }
            } else {
                Timber.d("Go to ChooseAccountFragment")
                viewModel.updateIsAccountConfirmed(false)
                if (MegaApplication.getChatManagement().isPendingJoinLink()) {
                    LoginActivity.isBackFromLoginPage = false
                    val intent = Intent(activity, ManagerActivity::class.java)
                    intent.action = Constants.ACTION_JOIN_OPEN_CHAT_LINK
                    intent.data = MegaApplication.getChatManagement().pendingJoinLink?.toUri()
                    activity.startActivity(intent)
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

                    intentAction?.let { action ->
                        when (action) {
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

                                if (loginUiState.rootNodesExists) {
                                    var newIntent =
                                        Intent(activity, ManagerActivity::class.java)

                                    when (action) {
                                        Constants.ACTION_FILE_PROVIDER -> {
                                            newIntent =
                                                Intent(
                                                    activity,
                                                    FileProviderActivity::class.java
                                                )
                                            intentExtras?.let { newIntent.putExtras(it) }
                                            newIntent.data = intentData
                                        }

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
                                            newIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            intentAction = Constants.ACTION_OPEN_CONTACTS_SECTION
                                            if (newIntent.getLongExtra(
                                                    Constants.CONTACT_HANDLE,
                                                    -1
                                                ) != -1L
                                            ) {
                                                newIntent.putExtra(
                                                    Constants.CONTACT_HANDLE,
                                                    newIntent.getLongExtra(
                                                        Constants.CONTACT_HANDLE,
                                                        -1
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    newIntent.action = intentAction

                                    intentDataString?.let { newIntent.data = it.toUri() }
                                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                                    activity.startActivity(newIntent)
                                    activity.finish()
                                } else {
                                    viewModel.fastLogin(activity.intent?.action == Constants.ACTION_REFRESH_API_SERVER)
                                }

                                return@apply
                            }
                        }
                    }

                    if (loginUiState.rootNodesExists && loginUiState.fetchNodesUpdate == null && !MegaApplication.isIsHeartBeatAlive) {
                        Timber.d("rootNode != null")

                        var newIntent = Intent(activity, ManagerActivity::class.java)

                        intentAction?.let { action ->
                            when (action) {
                                Constants.ACTION_FILE_PROVIDER -> {
                                    newIntent =
                                        Intent(activity, FileProviderActivity::class.java)
                                    intentExtras?.let { newIntent.putExtras(it) }
                                    newIntent.data = intentData
                                }

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
                            intentDataString?.let { newIntent.data = it.toUri() }
                        }

                        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                        activity.startActivity(newIntent)
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
                    val newIntent: Intent
                    when (action) {
                        Constants.ACTION_FILE_PROVIDER -> {
                            newIntent = Intent(activity, FileProviderActivity::class.java)
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
    }

    LaunchedEffect(Unit) {
        activity.intent?.let { intent ->
            intentAction = intent.action

            intentAction?.let { action ->
                Timber.d("action is: %s", action)
                when (action) {
                    Constants.ACTION_CONFIRM -> {
                        Timber.d("querySignupLink")
                        intent.getStringExtra(Constants.EXTRA_CONFIRMATION)
                            ?.let { viewModel.checkSignupLink(it) }
                        return@LaunchedEffect
                    }

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
                                AccountBlockedDetail(
                                    accountBlockedType,
                                    accountBlockedString
                                )
                            )
                        }
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
): Intent {
    var intent = Intent(activity, ManagerActivity::class.java)
    if (intentAction != null) {
        Timber.d("The action is: %s", intentAction)
        intent.action = intentAction
        when (intentAction) {
            Constants.ACTION_FILE_PROVIDER -> {
                intent = Intent(activity, FileProviderActivity::class.java)
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
                intent.putExtra(
                    Constants.CONTACT_HANDLE,
                    activity.intent?.getLongExtra(Constants.CONTACT_HANDLE, -1),
                )
            }
        }
        intentDataString?.let { intent.data = it.toUri() }
    } else {
        Timber.w("The intent action is NULL")
    }
    return intent
}


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