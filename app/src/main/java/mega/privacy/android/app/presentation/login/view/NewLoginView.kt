package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.apiserver.view.NewChangeApiServerDialog
import mega.privacy.android.app.presentation.extensions.login.newError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginTestTags.ACCOUNT_BLOCKED_DIALOG
import mega.privacy.android.app.presentation.login.view.LoginTestTags.ACCOUNT_LOCKED_DIALOG
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.legacy.core.ui.controls.keyboard.keyboardAsState
import mega.privacy.android.shared.original.core.ui.model.KeyboardState
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ForgotPasswordButtonPressedEvent
import mega.privacy.mobile.analytics.event.LoginButtonPressedEvent
import mega.privacy.mobile.analytics.event.LoginHelpButtonPressedEvent
import mega.privacy.mobile.analytics.event.SignUpButtonOnLoginPagePressedEvent

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
                                    painter = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_help_circle_medium_regular_outline),
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
                showLoginInProgress -> LoginInProgress(
                    state = this,
                    modifier = Modifier.padding(paddingValues)
                )

                isLoginRequired -> RequireLogin(
                    state = this,
                    snackbarHostState = snackbarHostState,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onLoginClicked = onLoginClicked,
                    onForgotPassword = onForgotPassword,
                    onCreateAccount = onCreateAccount,
                    onChangeApiServer = { showChangeApiServerDialog = true },
                    modifier = Modifier.padding(paddingValues),
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

@Composable
private fun RequireLogin(
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
    var accountBlockedDetail by remember { mutableStateOf<AccountBlockedDetail?>(null) }
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
        } else if (state.loginException is LoginTooManyAttempts) {
            tooManyAttempts = true
        }
        onLoginExceptionConsumed()
    }

    EventEffect(event = state.accountBlockedEvent, onConsumed = onResetAccountBlockedEvent) {
        accountBlockedDetail = it
    }

    val resendVerificationEmailSuccessMessage =
        stringResource(sharedR.string.general_email_resend_success_message)
    val resendVerificationEmailFailureMessage =
        stringResource(sharedR.string.general_request_failed_message)

    EventEffect(
        event = state.resendVerificationEmailEvent,
        onConsumed = onResetResendVerificationEmailEvent
    ) {
        val message = if (it) {
            resendVerificationEmailSuccessMessage
        } else {
            resendVerificationEmailFailureMessage
        }
        accountBlockedDetail = null
        stopLogin()
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
    }

    Box(
        modifier = modifier
            .verticalScroll(scrollState)
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
            modifier = contentModifier,
        ) {
            Image(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                painter = painterResource(id = R.drawable.ic_mega_login),
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
                text = stringResource(sharedR.string.login_page_title),
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
                label = stringResource(id = sharedR.string.email_text),
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
                label = stringResource(id = sharedR.string.password_text),
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
                    body = stringResource(sharedR.string.login_wrong_credential_error_message),
                    showCancelButton = false
                )
            }

            if (tooManyAttempts) {
                InlineErrorBanner(
                    modifier = Modifier
                        .testTag(LoginTestTags.TOO_MANY_ATTEMPTS_BANNER)
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    body = stringResource(sharedR.string.login_too_many_attempts_error_message),
                    showCancelButton = false
                )
            }


            PrimaryFilledButton(
                modifier = Modifier
                    .testTag(LoginTestTags.LOGIN_BUTTON)
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = sharedR.string.login_text),
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
                text = stringResource(sharedR.string.login_page_forgot_password_text),
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
                value = stringResource(sharedR.string.login_page_sign_up_action_footer_text),
                spanStyles = hashMapOf(
                    SpanIndicator('A') to SpanStyleWithAnnotation(
                        MegaSpanStyle.LinkColorStyle(
                            SpanStyle(),
                            LinkColor.Primary
                        ),
                        stringResource(sharedR.string.login_page_sign_up_action_footer_text)
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

            Spacer(modifier = Modifier.height(56.dp))

            accountBlockedDetail?.let {
                if (it.type == AccountBlockedType.TOS_COPYRIGHT || it.type == AccountBlockedType.TOS_NON_COPYRIGHT || it.type == AccountBlockedType.SUBUSER_DISABLED) {
                    BasicDialog(
                        modifier = Modifier.testTag(ACCOUNT_BLOCKED_DIALOG),
                        title = stringResource(id = sharedR.string.general_unable_to_login),
                        description = it.text,
                        positiveButtonText = stringResource(id = sharedR.string.document_scanning_error_dialog_confirm_button),
                        onPositiveButtonClicked = {
                            accountBlockedDetail = null
                        }
                    )
                } else if (it.type == AccountBlockedType.VERIFICATION_EMAIL) {
                    BasicDialog(
                        modifier = Modifier.testTag(ACCOUNT_LOCKED_DIALOG),
                        title = stringResource(id = sharedR.string.general_check_inbox),
                        description = it.text,
                        negativeButtonText = stringResource(id = sharedR.string.document_scanning_error_dialog_confirm_button),
                        onNegativeButtonClicked = {
                            accountBlockedDetail = null
                            stopLogin()
                        },
                        positiveButtonText = stringResource(id = sharedR.string.general_resend_email),
                        onPositiveButtonClicked = onResendVerificationEmail
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginInProgress(
    state: LoginState,
    modifier: Modifier = Modifier,
) {
    val isInLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = stringResource(id = R.string.login_to_mega),
            modifier = Modifier
                .align(Alignment.Center)
                .size(288.dp)
                .testTag(MEGA_LOGO_TEST_TAG),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 56.dp, end = 56.dp)
                    .widthIn(max = 300.dp)
            ) {
                MegaAnimatedLinearProgressIndicator(
                    indicatorProgress = state.currentProgress,
                    progressAnimDuration = if (state.currentProgress > 0.5f) 1000 else 3000,
                    modifier = Modifier
                        .testTag(FETCH_NODES_PROGRESS_TEST_TAG)
                )

                if (state.isRequestStatusInProgress) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "Request Status Progress")
                    val shimmerTranslateX by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "Progress"
                    )
                    // Shimmer Effect
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(fraction = state.currentProgress + 0.015f)
                            .clip(RoundedCornerShape(20.dp))
                            .graphicsLayer(translationX = shimmerTranslateX * 700f)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(
                                        100f,
                                        0f
                                    )
                                ),
                                shape = RoundedCornerShape(40.dp)
                            )
                            .testTag(REQUEST_STATUS_PROGRESS_TEST_TAG)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box {
                LoginInProgressText(
                    stringId = state.currentStatusText,
                    progress = state.requestStatusProgress,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                        )
                        .testTag(CONNECTING_TO_SERVER_TAG)
                )
                // White-space to prevent jumping when visibility animates
                MegaText(
                    text = " ",
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                    minLines = 2
                )
            }
            Spacer(modifier = Modifier.height(if (isInLandscape) 0.dp else 20.dp))
        }
    }
}

/**
 * Composable to show current status text with a fade in/out animation.
 */
@Composable
private fun LoginInProgressText(
    modifier: Modifier,
    @StringRes stringId: Int,
    progress: Progress? = null,
    textChangeDuration: Long = 200,
) {
    val isInPreview = LocalInspectionMode.current // To avoid text being hidden in previews
    var visible by rememberSaveable { mutableStateOf(isInPreview) }
    var currentTextId by rememberSaveable { mutableIntStateOf(stringId) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        MegaText(
            text = if (progress != null) {
                stringResource(sharedR.string.login_completing_operation, progress.intValue)
            } else {
                stringResource(id = currentTextId)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
            textAlign = TextAlign.Center,
            textColor = TextColor.Primary,
            minLines = 2
        )
    }

    LaunchedEffect(stringId) {
        visible = false
        delay(textChangeDuration)
        currentTextId = stringId
        visible = true
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

private const val LONG_PRESS_DELAY = 5000L
