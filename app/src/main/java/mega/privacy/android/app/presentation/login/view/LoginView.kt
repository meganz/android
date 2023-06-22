package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.login.error
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import mega.privacy.android.app.presentation.twofactorauthentication.view.TwoFactorAuthenticationField
import mega.privacy.android.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.textfields.LabelTextField
import mega.privacy.android.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_200_grey_700
import mega.privacy.android.core.ui.theme.extensions.red_600_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate

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
 * @param on2FAPinChanged           Action when one of the 2FA's pins changed.
 * @param on2FAChanged              Action when a 2FA code was pasted.
 * @param onLostAuthenticatorDevice Action when Lost authenticator device is pressed.
 * @param onBackPressed             Action when back is pressed.
 * @param onUpdateKarereLogs        Action when needs to update MegaChat logs.
 * @param onUpdateSdkLogs           Action when needs to update Sdk logs.
 * @param onChangeApiServer         Action when needs to update API server.
 * @param onFirstTime2FAConsumed    Action when the 2FA is shown for the first time.
 * @param modifier                  [Modifier]
 */
@Composable
fun LoginView(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onSnackbarMessageConsumed: () -> Unit,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onBackPressed: () -> Unit,
    onUpdateKarereLogs: () -> Unit,
    onUpdateSdkLogs: () -> Unit,
    onChangeApiServer: () -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            if (state.is2FARequired || state.multiFactorAuthState != null) {
                SimpleTopAppBar(
                    titleId = R.string.login_verification,
                    elevation = scrollState.value > 0,
                    onBackPressed = onBackPressed
                )
            }
        },
    ) { paddingValues ->
        with(state) {
            when {
                isLoginInProgress || fetchNodesUpdate != null -> LoginInProgress(
                    state = this,
                    paddingValues = paddingValues
                )

                isLoginRequired -> RequireLogin(
                    state = this,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClicked = onLoginClicked,
                    onForgotPassword = onForgotPassword,
                    onCreateAccount = onCreateAccount,
                    paddingValues = paddingValues,
                    onUpdateKarereLogs = onUpdateKarereLogs,
                    onUpdateSdkLogs = onUpdateSdkLogs,
                    onChangeApiServer = onChangeApiServer,
                    modifier = modifier,
                )

                is2FARequired || multiFactorAuthState != null -> TwoFactorAuthentication(
                    state = this,
                    paddingValues = paddingValues,
                    on2FAPinChanged = on2FAPinChanged,
                    on2FAChanged = on2FAChanged,
                    onLostAuthenticatorDevice = onLostAuthenticatorDevice,
                    onFirstTime2FAConsumed = onFirstTime2FAConsumed
                )
            }
        }

        BackHandler { onBackPressed() }

        SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackbarHostState)

        val context = LocalContext.current

        EventEffect(
            event = state.snackbarMessage,
            onConsumed = onSnackbarMessageConsumed
        ) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = context.resources.getString(it),
                duration = SnackbarDuration.Long
            )
        }
    }
}

@Composable
private fun RequireLogin(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    paddingValues: PaddingValues,
    onUpdateKarereLogs: () -> Unit,
    onUpdateSdkLogs: () -> Unit,
    onChangeApiServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(paddingValues)
            .verticalScroll(scrollState)
    ) {
        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }
        var pendingClicksKarere by remember { mutableStateOf(CLICKS_TO_ENABLE_LOGS) }
        var pendingClicksSDK by remember { mutableStateOf(CLICKS_TO_ENABLE_LOGS) }
        val focusManager = LocalFocusManager.current

        Text(
            modifier = Modifier
                .padding(start = 22.dp, top = 17.dp, end = 22.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        val downTime = System.currentTimeMillis()
                        tryAwaitRelease()
                        val upTime = System.currentTimeMillis()
                        if (upTime - downTime >= LONG_PRESS_DELAY) {
                            onChangeApiServer()
                        } else {
                            pendingClicksKarere = if (pendingClicksKarere == 1) {
                                onUpdateKarereLogs()
                                CLICKS_TO_ENABLE_LOGS
                            } else {
                                pendingClicksKarere - 1
                            }
                        }
                    })
                },
            text = stringResource(id = R.string.login_to_mega),
            style = MaterialTheme.typography.subtitle1.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.textColorPrimary
            ),
        )
        LabelTextField(
            onTextChange = onEmailChanged,
            label = stringResource(id = R.string.email_text),
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            modifier = Modifier
                .padding(start = 22.dp, top = 44.dp, end = 22.dp)
                .focusRequester(emailFocusRequester)
                .focusProperties {
                    next = passwordFocusRequester
                    previous = emailFocusRequester
                },
            text = state.accountSession?.email ?: "",
            errorText = state.emailError?.let { stringResource(id = it.error) },
            isEmail = true
        )
        PasswordTextField(
            onTextChange = onPasswordChanged,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                clickLogin(
                    onLoginClicked,
                    focusManager
                )
            }),
            modifier = Modifier
                .padding(
                    start = 22.dp,
                    top = if (state.emailError != null) 24.dp else 44.dp,
                    end = 22.dp
                )
                .focusRequester(passwordFocusRequester)
                .focusProperties {
                    next = passwordFocusRequester
                    previous = emailFocusRequester
                },
            text = state.password ?: "",
            errorText = state.passwordError?.let { stringResource(id = it.error) }
        )
        RaisedDefaultMegaButton(
            modifier = Modifier.padding(
                start = 22.dp,
                top = if (state.passwordError != null) 24.dp else 44.dp,
                end = 22.dp
            ),
            textId = R.string.login_text,
            onClick = { clickLogin(onLoginClicked, focusManager) },
            enabled = !state.isLocalLogoutInProgress
        )
        Row(
            modifier = Modifier
                .padding(start = 22.dp, top = 10.dp, end = 22.dp)
                .alpha(if (state.isLocalLogoutInProgress) 1f else 0f)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 3.dp),
                color = MaterialTheme.colors.secondary,
                strokeWidth = 1.dp
            )
            Text(
                text = stringResource(id = R.string.login_in_progress),
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.secondary)
            )
        }
        TextMegaButton(
            modifier = Modifier.padding(start = 14.dp, top = 34.dp),
            textId = R.string.forgot_pass,
            onClick = onForgotPassword
        )
        Row(modifier = Modifier.padding(end = 22.dp)) {
            Text(
                modifier = Modifier
                    .padding(start = 22.dp, top = 18.dp)
                    .clickable {
                        pendingClicksSDK = if (pendingClicksSDK == 1) {
                            onUpdateSdkLogs()
                            CLICKS_TO_ENABLE_LOGS
                        } else {
                            pendingClicksSDK - 1
                        }
                    },
                text = stringResource(id = R.string.new_to_mega),
                style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorPrimary),
            )
            TextMegaButton(
                modifier = Modifier.padding(top = 4.dp),
                textId = R.string.create_account,
                onClick = onCreateAccount
            )
        }
    }
}


private fun clickLogin(onLoginClicked: () -> Unit, focusManager: FocusManager) {
    focusManager.clearFocus(true)
    onLoginClicked()
}

@Composable
private fun LoginInProgress(
    state: LoginState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_loading_ic),
            contentDescription = stringResource(id = R.string.login_to_mega),
            modifier = Modifier
                .padding(top = 112.dp)
                .size(144.dp)
                .testTag(MEGA_LOGO_TEST_TAG),
            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.red_600_white_alpha_087)
        )
        with(state) {
            if (isCheckingSignupLink) {
                LoginInProgressText(
                    stringId = R.string.login_querying_signup_link,
                    modifier = Modifier.padding(top = 30.dp)
                )
            }
            LoginInProgressText(
                stringId = R.string.login_connecting_to_server,
                modifier = Modifier.padding(top = 5.dp)
            )
            fetchNodesUpdate?.apply {
                LoginInProgressText(
                    stringId = R.string.download_updating_filelist,
                    modifier = Modifier.padding(top = 5.dp)
                )
                progress?.let {
                    if (it.floatValue > 0) {
                        LoginInProgressText(
                            stringId = R.string.login_preparing_filelist,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                        LinearProgressIndicator(
                            progress = it.floatValue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                                .testTag(FETCH_NODES_PROGRESS_TEST_TAG),
                            backgroundColor = MaterialTheme.colors.grey_200_grey_700,
                            color = MaterialTheme.colors.secondary
                        )
                    }
                }
            }
            CircularProgress(testTag = LOGIN_PROGRESS_TEST_TAG)
            fetchNodesUpdate?.temporaryError?.let {
                LoginInProgressText(
                    stringId = it.messageId,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun LoginInProgressText(
    @StringRes stringId: Int,
    modifier: Modifier,
) = Text(
    text = stringResource(id = stringId),
    modifier = modifier,
    style = MaterialTheme.typography.subtitle2,
    textAlign = TextAlign.Center
)

@Composable
private fun TwoFactorAuthentication(
    state: LoginState,
    paddingValues: PaddingValues,
    on2FAPinChanged: (String, Int) -> Unit,
    on2FAChanged: (String) -> Unit,
    onLostAuthenticatorDevice: () -> Unit,
    onFirstTime2FAConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .fillMaxWidth()
        .padding(paddingValues)
) {
    val scrollState = rememberScrollState()
    val isChecking2FA = state.multiFactorAuthState == MultiFactorAuthState.Checking
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isError = state.multiFactorAuthState == MultiFactorAuthState.Failed
        Text(
            text = stringResource(id = R.string.explain_confirm_2fa),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 40.dp),
            style = MaterialTheme.typography.subtitle1.copy(color = MaterialTheme.colors.textColorSecondary),
            textAlign = TextAlign.Center
        )
        TwoFactorAuthenticationField(
            twoFAPin = state.twoFAPin,
            isError = isError,
            on2FAPinChanged = on2FAPinChanged,
            on2FAChanged = on2FAChanged,
            requestFocus = state.isFirstTime2FA,
            onRequestFocusConsumed = onFirstTime2FAConsumed
        )
        if (isError) {
            Text(
                text = stringResource(id = R.string.pin_error_2fa),
                modifier = Modifier.padding(start = 10.dp, top = 18.dp, end = 10.dp),
                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.error)
            )
        }
        TextMegaButton(
            textId = R.string.lost_your_authenticator_device,
            onClick = onLostAuthenticatorDevice,
            modifier = Modifier.padding(top = if (isError) 0.dp else 29.dp, bottom = 40.dp)
        )
    }

    if (isChecking2FA) {
        CircularProgress(
            testTag = TWO_FA_PROGRESS_TEST_TAG,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun CircularProgress(testTag: String, modifier: Modifier = Modifier) =
    CircularProgressIndicator(
        modifier = modifier
            .padding(top = 10.dp)
            .size(72.dp)
            .testTag(testTag),
        color = MaterialTheme.colors.secondary
    )

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewEmptyLoginView")
@Composable
private fun PreviewEmptyLoginView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var state by remember { mutableStateOf(LoginState(isLoginRequired = true)) }

        RequireLogin(
            state = state,
            onEmailChanged = {
                state = state.copy(accountSession = AccountSession(email = it))
            },
            onPasswordChanged = { state = state.copy(password = it) },
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            paddingValues = PaddingValues(),
            onUpdateKarereLogs = {},
            onUpdateSdkLogs = {},
            onChangeApiServer = {},
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewLoginView")
@Composable
private fun PreviewLoginView(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LoginView(
            state = state,
            onEmailChanged = {},
            onPasswordChanged = {},
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            onSnackbarMessageConsumed = {},
            on2FAPinChanged = { _, _ -> },
            on2FAChanged = {},
            onLostAuthenticatorDevice = {},
            onBackPressed = {},
            onUpdateKarereLogs = {},
            onUpdateSdkLogs = {},
            onChangeApiServer = {},
            onFirstTime2FAConsumed = {}
        )
    }
}

/**
 * LoginState parameter provider for compose previews.
 */
internal class LoginStateProvider : PreviewParameterProvider<LoginState> {
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
            fetchNodesUpdate = FetchNodesUpdate(
                progress = Progress(0.5F),
                temporaryError = FetchNodesTemporaryError.ConnectivityIssues
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

internal const val MEGA_LOGO_TEST_TAG = "MEGA_LOGO"
internal const val FETCH_NODES_PROGRESS_TEST_TAG = "FETCH_NODES_PROGRESS"
internal const val LOGIN_PROGRESS_TEST_TAG = "LOGIN_PROGRESS"
internal const val TWO_FA_PROGRESS_TEST_TAG = "TWO_FA_PROGRESS"
internal const val CLICKS_TO_ENABLE_LOGS = 5
private const val LONG_PRESS_DELAY = 5000L
