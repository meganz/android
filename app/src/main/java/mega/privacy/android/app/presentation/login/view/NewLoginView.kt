package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.apiserver.view.NewChangeApiServerDialog
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.LoginHelpButtonPressedEvent

/**
 * Login fragment view.
 *
 * @param state                     [LoginState]
 * @param onEmailChanged            Action when the typed email changes.
 * @param onPasswordChanged         Action when the typed password changes.
 * @param onLoginClicked            Action when Login is pressed.
 * @param onForgotPassword          Action when Forgot password is pressed.
 * @param onCreateAccount           Action when Create account is pressed.
 * @param onSnackbarMessageConsumed Action when the snackbar message was consumed.
 * @param on2FAChanged              Action when a 2FA code was pasted.
 * @param onLostAuthenticatorDevice Action when Lost authenticator device is pressed.
 * @param onBackPressed             Action when back is pressed.
 * @param modifier                  [Modifier]
 */
@Composable
fun NewLoginView(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onSnackbarMessageConsumed: () -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onBackPressed: () -> Unit,
    onReportIssue: () -> Unit,
    onResetAccountBlockedEvent: () -> Unit,
    onResendVerificationEmail: () -> Unit,
    onResetResendVerificationEmailEvent: () -> Unit,
    stopLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginExceptionConsumed: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showChangeApiServerDialog by rememberSaveable { mutableStateOf(false) }
    val showLoginInProgress =
        state.isLoginInProgress || state.fetchNodesUpdate != null || state.isRequestStatusInProgress
    val orientation = LocalConfiguration.current.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val requiredLoginScrollState = rememberScrollState()
    val twoFactorAuthScrollState = rememberScrollState()
    var loginTopBarTitle by rememberSaveable {
        mutableStateOf("")
    }
    val loginText = stringResource(sharedR.string.login_text)

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .conditional(!showLoginInProgress) {
                statusBarsPadding()
            }
            .semantics { testTagsAsResourceId = true },
        snackbarHost = {
            MegaSnackbar(snackbarHostState)
        },
        topBar = {
            if (state.is2FARequired && !showLoginInProgress) {
                AnimatedVisibility(
                    visible = !isPhoneLandscape || !twoFactorAuthScrollState.canScrollBackward,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                ) {
                    MegaTopAppBar(
                        navigationType = AppBarNavigationType.Back(onBackPressed),
                        title = stringResource(sharedR.string.settings_2fa),
                    )
                }
            } else if (!showLoginInProgress) {
                AnimatedVisibility(
                    visible = !isPhoneLandscape || !requiredLoginScrollState.canScrollBackward,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it }),
                ) {
                    MegaTopAppBar(
                        title = if (state.isLoginRequired) loginTopBarTitle else "",
                        navigationType = AppBarNavigationType.Back {
                            onBackPressed()
                            focusManager.clearFocus(true)
                        },
                        trailingIcons = {
                            IconButton(
                                onClick = {
                                    onReportIssue()
                                    Analytics.tracker.trackEvent(LoginHelpButtonPressedEvent)
                                },
                            ) {
                                MegaIcon(
                                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
                                    tint = IconColor.Primary,
                                    contentDescription = "Report issue Icon"
                                )
                            }
                        }
                    )
                }
            }
        },
    ) { paddingValues ->
        with(state) {
            when {
                showLoginInProgress -> LoginInProgressContent(
                    modifier = Modifier.padding(paddingValues),
                    currentProgress = state.currentProgress,
                    currentStatusText = state.currentStatusText,
                    requestStatusProgress = state.requestStatusProgress
                )

                isLoginRequired -> RequireLogin(
                    state = this,
                    paddingValues = paddingValues,
                    snackbarHostState = snackbarHostState,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClicked = onLoginClicked,
                    onForgotPassword = onForgotPassword,
                    onCreateAccount = onCreateAccount,
                    onChangeApiServer = { showChangeApiServerDialog = true },
                    onToggleTitle = { isHidden ->
                        loginTopBarTitle = if (isHidden) loginText else ""
                    },
                    scrollState = requiredLoginScrollState,
                    onLoginExceptionConsumed = onLoginExceptionConsumed,
                    onResendVerificationEmail = onResendVerificationEmail,
                    onResetAccountBlockedEvent = onResetAccountBlockedEvent,
                    onResetResendVerificationEmailEvent = onResetResendVerificationEmailEvent,
                    stopLogin = stopLogin,
                )

                is2FARequired || multiFactorAuthState != null -> NewTwoFactorAuthentication(
                    state = this,
                    on2FAChanged = on2FAChanged,
                    onLostAuthenticatorDevice = onLostAuthenticatorDevice,
                    modifier = Modifier
                        .verticalScroll(twoFactorAuthScrollState)
                        .padding(paddingValues),
                )
            }
        }

        val context = LocalContext.current

        EventEffect(
            event = state.snackbarMessage,
            onConsumed = onSnackbarMessageConsumed
        ) {
            snackbarHostState.showSnackbar(
                message = context.resources.getString(it),
                duration = SnackbarDuration.Short
            )
        }

        if (showChangeApiServerDialog) {
            NewChangeApiServerDialog(onDismissRequest = { showChangeApiServerDialog = false })
        }
    }
}

internal fun tabletScreenWidth(orientation: Int): Dp {
    return when (orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> screenWidthTabletLandscape.dp
        Configuration.ORIENTATION_PORTRAIT -> screenWidthTabletPortrait.dp
        else -> screenWidthTabletPortrait.dp
    }
}

internal const val screenWidthTabletPortrait = 348
internal const val screenWidthTabletLandscape = 357

internal object LoginTestTags {
    private const val LOGIN_SCREEN = "login_screen"
    const val EMAIL_INPUT = "${LOGIN_SCREEN}:email_input"
    const val PASSWORD_INPUT = "${LOGIN_SCREEN}:password_input"
    const val LOGIN_BUTTON = "${LOGIN_SCREEN}:login_button"
    const val FORGOT_PASSWORD_BUTTON = "${LOGIN_SCREEN}:forgot_password_button"
    const val SIGN_UP_BUTTON = "${LOGIN_SCREEN}:sign_up_button"
    const val ACCOUNT_BLOCKED_DIALOG = "${LOGIN_SCREEN}:account_blocked_dialog"
    const val ACCOUNT_LOCKED_DIALOG = "${LOGIN_SCREEN}:account_locked_dialog"
    const val WRONG_CREDENTIAL_BANNER = "${LOGIN_SCREEN}:wrong_credential_banner"
    const val TOO_MANY_ATTEMPTS_BANNER = "${LOGIN_SCREEN}:too_many_attempts_banner"
}

@CombinedThemePreviews
@Composable
private fun EmptyLoginViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var state by remember { mutableStateOf(LoginState(isLoginRequired = true)) }

        RequireLogin(
            state = state,
            snackbarHostState = SnackbarHostState(),
            onEmailChanged = {
                state = state.copy(accountSession = AccountSession(email = it))
            },
            onToggleTitle = {},
            onPasswordChanged = { state = state.copy(password = it) },
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onChangeApiServer = {},
            onResetAccountBlockedEvent = {},
            onResendVerificationEmail = {},
            onLoginExceptionConsumed = {},
            onResetResendVerificationEmailEvent = {},
            stopLogin = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LoginViewPreview(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewLoginView(
            state = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onSnackbarMessageConsumed = {},
            on2FAChanged = {},
            onLostAuthenticatorDevice = {},
            onBackPressed = {},
            onReportIssue = {},
            onResetAccountBlockedEvent = {},
            onResetResendVerificationEmailEvent = {},
            onResendVerificationEmail = {},
            stopLogin = {}
        )
    }
}

@CombinedThemePreviewsTablet
@Composable
private fun LandscapeLoginViewPreview(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewLoginView(
            state = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onSnackbarMessageConsumed = {},
            on2FAChanged = {},
            onLostAuthenticatorDevice = {},
            onBackPressed = {},
            onReportIssue = {},
            onResetAccountBlockedEvent = {},
            onResetResendVerificationEmailEvent = {},
            onResendVerificationEmail = {},
            stopLogin = {}
        )
    }
}


/**
 * LoginState parameter provider for compose previews.
 */
private class LoginStateProvider : PreviewParameterProvider<LoginState> {
    override val values = listOf(
        LoginState(
            isLoginRequired = true,
            accountSession = AccountSession(email = "email@email.es"),
            password = "Password",
            isLocalLogoutInProgress = true
        ),
        LoginState(
            isLoginRequired = true,
            accountSession = AccountSession(email = ""),
            emailError = LoginError.EmptyEmail,
            password = "",
            passwordError = LoginError.EmptyPassword
        ),
        LoginState(
            isLoginInProgress = true,
        ),
        LoginState(
            isLoginInProgress = true,
            requestStatusProgress = Progress(0.2f)
        ),
        LoginState(
            isLoginInProgress = true,
            requestStatusProgress = Progress(0.7f)
        ),
        LoginState(
            fetchNodesUpdate = FetchNodesUpdate(
                progress = Progress(0.5F),
                temporaryError = TemporaryWaitingError.ConnectivityIssues
            ),
        ),
        LoginState(
            is2FARequired = true,
            twoFAPin = listOf("1", "2", "", "", "", "")
        ),
        LoginState(
            multiFactorAuthState = MultiFactorAuthState.Failed,
            twoFAPin = listOf("1", "2", "3", "4", "5", "6")
        ),
        LoginState(
            multiFactorAuthState = MultiFactorAuthState.Checking,
            twoFAPin = listOf("1", "2", "3", "4", "5", "6")
        ),
    ).asSequence()
}

internal const val TWO_FA_PROGRESS_TEST_TAG = "TWO_FA_PROGRESS"
internal const val ENTER_AUTHENTICATION_CODE_TAG =
    "two_factor_authentication:text_enter_authentication_code"
internal const val INVALID_CODE_TAG =
    "two_factor_authentication:text_invalid_code"
internal const val LOST_AUTHENTICATION_CODE_TAG =
    "two_factor_authentication:text_mega_button_lost_authentication_code"

