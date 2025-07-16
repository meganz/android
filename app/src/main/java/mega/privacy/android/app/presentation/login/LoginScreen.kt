package mega.privacy.android.app.presentation.login

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.view.NewLoginView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL
import mega.privacy.android.app.utils.ConstantsUrl.RECOVERY_URL_EMAIL
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LoginScreenEvent
import nz.mega.sdk.MegaError
import timber.log.Timber

/**
 * Login fragment.
 *
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    billingViewModel: BillingViewModel
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showIncorrectRkDialog by rememberSaveable { mutableStateOf(false) }
    var recoveryKeyInput by rememberSaveable(uiState.recoveryKeyLink) { mutableStateOf("") }
    var recoveryKeyError by rememberSaveable(uiState.recoveryKeyLink) {
        mutableStateOf<String?>(null)
    }

    LoginIntentActionHandler(viewModel = viewModel, uiState = uiState)

    EventEffect(uiState.checkRecoveryKeyEvent, viewModel::onCheckRecoveryKeyEventConsumed) {
        if (it.isSuccess) {
            val data = it.getOrThrow()
            navigateToChangePassword(
                context = context,
                link = data.link,
                value = data.recoveryKey
            )
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

    BackHandler {
        with(uiState) {
            when {
                Constants.ACTION_REFRESH == activity?.intent?.action || Constants.ACTION_REFRESH_API_SERVER == activity?.intent?.action ->
                    return@BackHandler

                is2FARequired || multiFactorAuthState != null -> {
                    viewModel.stopLogin()
                }

                viewModel.loginMutex.isLocked || isLoginInProgress || isFastLoginInProgress || fetchNodesUpdate != null ->
                    activity?.moveTaskToBack(true)

                else -> {
                    LoginActivity.isBackFromLoginPage = true
                    viewModel.setPendingFragmentToShow(LoginFragmentType.Tour)
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
            onBackPressed = {
                onBackPressedDispatcher?.onBackPressed()
            },
            onReportIssue = {
                openLoginIssueHelpdeskPage(context)
            },
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

private fun openLoginIssueHelpdeskPage(context: Context) {
    context.launchUrl("https://help.mega.io/accounts/login-issues")
}

private fun navigateToChangePassword(context: Context, link: String, value: String) {
    val intent = Intent(context, ChangePasswordActivity::class.java)
    intent.action = Constants.ACTION_RESET_PASS_FROM_LINK
    intent.data = link.toUri()
    intent.putExtra(IntentConstants.EXTRA_MASTER_KEY, value)
    context.startActivity(intent)
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