package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.extensions.login.newError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.legacy.core.ui.controls.keyboard.keyboardAsState
import mega.privacy.android.shared.original.core.ui.model.KeyboardState
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.ForgotPasswordButtonPressedEvent
import mega.privacy.mobile.analytics.event.LoginButtonPressedEvent
import mega.privacy.mobile.analytics.event.SignUpButtonOnLoginPagePressedEvent

@Composable
fun RequireLogin(
    state: LoginState,
    snackbarHostState: SnackbarHostState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onChangeApiServer: () -> Unit,
    onResendVerificationEmail: () -> Unit,
    onResetAccountBlockedEvent: () -> Unit,
    onResetResendVerificationEmailEvent: () -> Unit,
    stopLogin: () -> Unit,
    onToggleTitle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onLoginExceptionConsumed: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val orientation = LocalConfiguration.current.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val keyboardState by keyboardAsState()
    val emailRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }
    var wrongCredentials by remember { mutableStateOf(false) }
    var tooManyAttempts by remember { mutableStateOf(false) }
    var accountBlockedEvent by remember { mutableStateOf<AccountBlockedEvent?>(null) }
    var titleOffset by remember { mutableIntStateOf(0) }
    var emailFieldOffset by remember { mutableIntStateOf(0) }
    var isEmailFieldFocused by remember { mutableStateOf(false) }
    val isTitleHidden by remember {
        derivedStateOf {
            titleOffset > scrollState.value
        }
    }

    LaunchedEffect(isTitleHidden) {
        onToggleTitle(!isTitleHidden)
    }

    LaunchedEffect(Unit) {
        delay(300L)
        emailRequester.requestFocus()
    }

    LaunchedEffect(isEmailFieldFocused, keyboardState) {
        if (isEmailFieldFocused && keyboardState == KeyboardState.Opened) {
            scrollState.animateScrollTo(emailFieldOffset)
        }
    }

    LaunchedEffect(state.loginException) {
        if (state.loginException is LoginWrongEmailOrPassword) {
            wrongCredentials = true
            onLoginExceptionConsumed()
        } else if (state.loginException is LoginTooManyAttempts) {
            tooManyAttempts = true
            onLoginExceptionConsumed()
        }
    }

    EventEffect(event = state.accountBlockedEvent, onConsumed = onResetAccountBlockedEvent) {
        accountBlockedEvent = it
    }

    val resendVerificationEmailSuccessMessage =
        stringResource(R.string.general_email_resend_success_message)
    val resendVerificationEmailFailureMessage =
        stringResource(R.string.general_request_failed_message)

    EventEffect(
        event = state.resendVerificationEmailEvent,
        onConsumed = onResetResendVerificationEmailEvent
    ) {
        val message = if (it) {
            resendVerificationEmailSuccessMessage
        } else {
            resendVerificationEmailFailureMessage
        }
        accountBlockedEvent = null
        stopLogin()
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
    }

    val minContentHeight = rememberMinContentHeight(paddingValues)

    Box(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(paddingValues)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus(true)
                })
            }, contentAlignment = Alignment.TopCenter
    ) {
        val contentModifier = if (isTablet || isPhoneLandscape) {
            Modifier
                .fillMaxHeight()
                .width(tabletScreenWidth(orientation))
                .padding(top = if (isTablet) 16.dp else 0.dp)
        } else {
            Modifier
                .fillMaxSize()
        }

        Column(
            modifier = contentModifier
                .defaultMinSize(minHeight = minContentHeight),
        ) {
            Image(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = mega.privacy.android.app.R.drawable.ic_mega_login),
                contentDescription = "Login Icon"
            )

            Spacer(modifier = Modifier.height(24.dp))

            MegaText(
                modifier = Modifier
                    .wrapContentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val downTime = System.currentTimeMillis()
                            tryAwaitRelease()
                            val upTime = System.currentTimeMillis()
                            if (upTime - downTime >= LONG_PRESS_DELAY) {
                                onChangeApiServer()
                            }
                        })
                    }
                    .padding(start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.login_page_title),
                style = AppTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                textColor = TextColor.Primary,
            )

            Spacer(
                modifier = Modifier
                    .height(40.dp)
                    .onGloballyPositioned { coordinates ->
                        // Store position of bottom of the title
                        titleOffset = coordinates.positionInParent().y.toInt()
                    }
            )

            TextInputField(
                modifier = Modifier
                    .testTag(LoginTestTags.EMAIL_INPUT)
                    .onGloballyPositioned { coordinates ->
                        emailFieldOffset = coordinates.positionInParent().y.toInt()
                    }
                    .focusRequester(emailRequester)
                    .focusProperties {
                        next = passwordRequester
                    }
                    .padding(start = 16.dp, end = 16.dp),
                onFocusChanged = { isFocused ->
                    isEmailFieldFocused = isFocused
                },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                label = stringResource(id = R.string.email_text),
                text = state.accountSession?.email.orEmpty(),
                onValueChanged = {
                    wrongCredentials = false
                    tooManyAttempts = false
                    onEmailChanged(it.trim())
                },
                errorText = when {
                    state.emailError != null -> stringResource(state.emailError.newError)
                    wrongCredentials -> ""
                    tooManyAttempts -> ""
                    else -> null
                },
                contentType = ContentType.EmailAddress
            )

            PasswordTextInputField(
                modifier = Modifier
                    .testTag(LoginTestTags.PASSWORD_INPUT)
                    .focusRequester(passwordRequester)
                    .focusProperties {
                        previous = emailRequester
                    }
                    .padding(
                        top = 16.dp, start = 16.dp, end = 16.dp
                    ),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        Analytics.tracker.trackEvent(LoginButtonPressedEvent)
                        focusManager.clearFocus(true)
                        onLoginClicked()
                    },
                ),
                label = stringResource(id = R.string.password_text),
                text = state.password.orEmpty(),
                onValueChanged = {
                    wrongCredentials = false
                    tooManyAttempts = false
                    onPasswordChanged(it)
                },
                errorText = when {
                    state.passwordError != null -> stringResource(state.passwordError.newError)
                    wrongCredentials -> ""
                    tooManyAttempts -> ""
                    else -> null
                }
            )
            if (wrongCredentials) {
                InlineErrorBanner(
                    modifier = Modifier
                        .testTag(LoginTestTags.WRONG_CREDENTIAL_BANNER)
                        .fillMaxWidth()
                        .padding(
                            top = 24.dp, start = 16.dp, end = 16.dp
                        ),
                    body = stringResource(R.string.login_wrong_credential_error_message),
                    showCancelButton = false
                )
            }

            if (tooManyAttempts) {
                InlineErrorBanner(
                    modifier = Modifier
                        .testTag(LoginTestTags.TOO_MANY_ATTEMPTS_BANNER)
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    body = stringResource(R.string.login_too_many_attempts_error_message),
                    showCancelButton = false
                )
            }


            PrimaryFilledButton(
                modifier = Modifier
                    .testTag(LoginTestTags.LOGIN_BUTTON)
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.login_text),
                isLoading = false,
                onClick = {
                    Analytics.tracker.trackEvent(LoginButtonPressedEvent)
                    focusManager.clearFocus(true)
                    onLoginClicked()
                },
            )

            TextOnlyButton(
                modifier = Modifier
                    .testTag(LoginTestTags.FORGOT_PASSWORD_BUTTON)
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(R.string.login_page_forgot_password_text),
                onClick = {
                    Analytics.tracker.trackEvent(ForgotPasswordButtonPressedEvent)
                    onForgotPassword()
                },
            )

            LinkSpannedText(
                modifier = Modifier
                    .testTag(LoginTestTags.SIGN_UP_BUTTON)
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = 40.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                    .imePadding(),
                value = stringResource(R.string.login_page_sign_up_action_footer_text),
                spanStyles = hashMapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(),
                            LinkColor.Primary
                        ),
                        stringResource(R.string.login_page_sign_up_action_footer_text)
                            .substringAfter("[A]")
                            .substringBefore("[/A]")
                    )
                ),
                baseStyle = AppTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                onAnnotationClick = {
                    Analytics.tracker.trackEvent(SignUpButtonOnLoginPagePressedEvent)
                    onCreateAccount()
                }
            )

            accountBlockedEvent?.let {
                if (it.type == AccountBlockedType.TOS_COPYRIGHT || it.type == AccountBlockedType.TOS_NON_COPYRIGHT || it.type == AccountBlockedType.SUBUSER_DISABLED) {
                    BasicDialog(
                        modifier = Modifier.testTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG),
                        title = stringResource(id = R.string.general_unable_to_login),
                        description = it.text,
                        positiveButtonText = stringResource(id = R.string.document_scanning_error_dialog_confirm_button),
                        onPositiveButtonClicked = {
                            accountBlockedEvent = null
                        }
                    )
                } else if (it.type == AccountBlockedType.VERIFICATION_EMAIL) {
                    BasicDialog(
                        modifier = Modifier.testTag(LoginTestTags.ACCOUNT_LOCKED_DIALOG),
                        title = stringResource(id = R.string.general_check_inbox),
                        description = it.text,
                        negativeButtonText = stringResource(id = R.string.document_scanning_error_dialog_confirm_button),
                        onNegativeButtonClicked = {
                            accountBlockedEvent = null
                            stopLogin()
                        },
                        positiveButtonText = stringResource(id = R.string.general_resend_email),
                        onPositiveButtonClicked = onResendVerificationEmail
                    )
                }
            }
        }
    }
}


/**
 * Calculates the minimum content height based on the screen height, padding values, and system bars.
 * This ensure container doesn't become scrollable if the content is smaller than the screen height.
 *
 * @param paddingValues The padding values to consider for the calculation.
 * @return the height as [Dp].
 */
@Composable
private fun rememberMinContentHeight(
    paddingValues: PaddingValues,
): Dp {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val systemBars = WindowInsets.systemBars
    return remember(density, windowInfo, systemBars, paddingValues) {
        with(density) {
            val windowHeight = windowInfo.containerSize.height
            val contentTopPadding = paddingValues.calculateTopPadding().toPx()
            val systemBarsPadding = systemBars.getTop(this) + systemBars.getBottom(this)
            (windowHeight - contentTopPadding - systemBarsPadding).toDp()
        }
    }
}

private const val LONG_PRESS_DELAY = 5000L