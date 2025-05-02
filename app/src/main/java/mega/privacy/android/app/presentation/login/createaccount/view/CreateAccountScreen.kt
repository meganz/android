package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.passwordstrength.MegaPasswordStrength
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaAnimatedLinearProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.LabelTextField
import mega.privacy.android.shared.original.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CreateAccountRoute(
    uiState: CreateAccountUIState,
    onNavigateToLogin: () -> Unit,
    openTermsAndServiceLink: () -> Unit,
    openEndToEndEncryptionLink: () -> Unit,
    setTemporalDataForAccountCreation: (EphemeralCredentials) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    CreateAccountScreen(
        uiState = uiState,
        onFirstNameInputChanged = viewModel::onFirstNameInputChanged,
        onLastNameInputChanged = viewModel::onLastNameInputChanged,
        onEmailInputChanged = viewModel::onEmailInputChanged,
        onPasswordInputChanged = viewModel::onPasswordInputChanged,
        onConfirmPasswordInputChanged = viewModel::onConfirmPasswordInputChanged,
        onCreateAccountClicked = viewModel::createAccount,
        onTermsOfServiceAgreedChanged = viewModel::termsOfServiceAgreedChanged,
        onE2EEAgreedChanged = viewModel::e2eeAgreedChanged,
        onLoginClicked = onNavigateToLogin,
        openTermsAndServiceLink = openTermsAndServiceLink,
        openEndToEndEncryptionLink = openEndToEndEncryptionLink,
        onResetShowAgreeToTermsEvent = viewModel::resetShowAgreeToTermsEvent,
        onCloseNetworkWarningClick = viewModel::networkWarningShown,
        onResetCreateAccountStatusEvent = viewModel::resetCreateAccountStatusEvent,
        onCreateAccountSuccess = {
            setTemporalDataForAccountCreation(it)
            viewModel.onCreateAccountSuccess(it)
        },
        modifier = modifier.semantics { testTagsAsResourceId = true },
    )
}

@Composable
internal fun CreateAccountScreen(
    uiState: CreateAccountUIState,
    onFirstNameInputChanged: (String) -> Unit,
    onLastNameInputChanged: (String) -> Unit,
    onEmailInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onConfirmPasswordInputChanged: (String) -> Unit,
    onCreateAccountClicked: () -> Unit,
    onTermsOfServiceAgreedChanged: (Boolean) -> Unit,
    onE2EEAgreedChanged: (Boolean) -> Unit,
    onLoginClicked: () -> Unit,
    openTermsAndServiceLink: () -> Unit,
    openEndToEndEncryptionLink: () -> Unit,
    onResetShowAgreeToTermsEvent: () -> Unit,
    onCloseNetworkWarningClick: () -> Unit,
    onResetCreateAccountStatusEvent: () -> Unit,
    onCreateAccountSuccess: (EphemeralCredentials) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState

    MegaScaffold(
        modifier = modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        scrollableContentState = scrollState,
        topBar = {
            if (!uiState.isLoading) {
                MegaAppBar(
                    modifier = Modifier.testTag(CREATE_ACCOUNT_TOOLBAR_TAG),
                    appBarType = AppBarType.NONE,
                    title = stringResource(R.string.create_account_title),
                    isStatusBarColorEnabled = false
                )
            }
        },
    ) { paddingValues ->
        CreateAccountView(
            uiState = uiState,
            scrollState = scrollState,
            snackbarHostState = snackbarHostState,
            onFirstNameInputChanged = onFirstNameInputChanged,
            onLastNameInputChanged = onLastNameInputChanged,
            onEmailInputChanged = onEmailInputChanged,
            onPasswordInputChanged = onPasswordInputChanged,
            onConfirmPasswordInputChanged = onConfirmPasswordInputChanged,
            onCreateAccountClicked = onCreateAccountClicked,
            onTermsOfServiceAgreedChanged = onTermsOfServiceAgreedChanged,
            onE2EEAgreedChanged = onE2EEAgreedChanged,
            onLoginClicked = onLoginClicked,
            openTermsAndServiceLink = openTermsAndServiceLink,
            openEndToEndEncryptionLink = openEndToEndEncryptionLink,
            onResetShowAgreeToTermsEvent = onResetShowAgreeToTermsEvent,
            onCloseNetworkWarningClick = onCloseNetworkWarningClick,
            onResetCreateAccountStatusEvent = onResetCreateAccountStatusEvent,
            onCreateAccountSuccess = onCreateAccountSuccess,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun CreateAccountView(
    uiState: CreateAccountUIState,
    scrollState: ScrollState,
    snackbarHostState: SnackbarHostState,
    onFirstNameInputChanged: (String) -> Unit,
    onLastNameInputChanged: (String) -> Unit,
    onEmailInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onConfirmPasswordInputChanged: (String) -> Unit,
    onCreateAccountClicked: () -> Unit,
    onTermsOfServiceAgreedChanged: (Boolean) -> Unit,
    onE2EEAgreedChanged: (Boolean) -> Unit,
    onLoginClicked: () -> Unit,
    openTermsAndServiceLink: () -> Unit,
    openEndToEndEncryptionLink: () -> Unit,
    onResetShowAgreeToTermsEvent: () -> Unit,
    onCloseNetworkWarningClick: () -> Unit,
    onResetCreateAccountStatusEvent: () -> Unit,
    onCreateAccountSuccess: (EphemeralCredentials) -> Unit,
    modifier: Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    var firstName by rememberSaveable { mutableStateOf<String?>(null) }
    var lastName by rememberSaveable { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf<String?>(null) }
    var password by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPassword by rememberSaveable { mutableStateOf<String?>(null) }
    var termsOfServiceAgreed by rememberSaveable { mutableStateOf(false) }
    var e2eeAgreed by rememberSaveable { mutableStateOf(false) }

    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val agreeToTermsMessage = stringResource(id = R.string.create_account_no_terms)
    val e2eeMessage = stringResource(id = R.string.create_account_no_top)
    val alreadyRegisteredMessage = stringResource(id = R.string.error_email_registered)

    EventEffect(
        event = uiState.showAgreeToTermsEvent,
        onConsumed = onResetShowAgreeToTermsEvent
    ) {
        val message =
            if (uiState.isTermsOfServiceAgreed != true) agreeToTermsMessage else if (uiState.isE2EEAgreed != true) e2eeMessage else null
        message?.let {
            coroutineScope.launch {
                snackbarHostState.showAutoDurationSnackbar(
                    message = message,
                )
            }
        }

    }

    EventEffect(
        event = uiState.createAccountStatusEvent,
        onConsumed = onResetCreateAccountStatusEvent
    ) { createAccountStatus ->
        when (createAccountStatus) {
            is CreateAccountStatus.Success -> {
                onCreateAccountSuccess(createAccountStatus.credentials)
            }

            is CreateAccountStatus.AccountAlreadyExists -> {
                coroutineScope.launch {
                    snackbarHostState.showAutoDurationSnackbar(
                        message = alreadyRegisteredMessage,
                    )
                }
            }

            is CreateAccountStatus.UnknownError -> {
                coroutineScope.launch {
                    snackbarHostState.showAutoDurationSnackbar(
                        message = createAccountStatus.message
                    )
                }
            }
        }
    }

    if (uiState.isLoading) {
        CreateAccountInProgressView(
            modifier = modifier
        )
    } else {
        Box {
            Column(
                modifier = modifier
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                LabelTextField(
                    onTextChange = {
                        firstName = it.trim()
                        onFirstNameInputChanged(firstName.orEmpty())
                    },
                    label = stringResource(id = R.string.first_name_text),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { lastNameFocusRequester.requestFocus() }),
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .focusRequester(firstNameFocusRequester)
                        .focusProperties {
                            next = lastNameFocusRequester
                            previous = firstNameFocusRequester
                        }
                        .testTag(CREATE_ACCOUNT_FIRST_NAME_TAG),
                    text = firstName.orEmpty(),
                    errorText = stringResource(id = R.string.error_enter_username).takeIf { uiState.isFirstNameValid == false },
                    isEmail = false
                )

                LabelTextField(
                    onTextChange = {
                        lastName = it.trim()
                        onLastNameInputChanged(lastName.orEmpty())
                    },
                    label = stringResource(id = R.string.lastname_text),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                        .focusRequester(lastNameFocusRequester)
                        .focusProperties {
                            next = emailFocusRequester
                            previous = firstNameFocusRequester
                        }
                        .testTag(CREATE_ACCOUNT_LAST_NAME_TAG),
                    text = lastName.orEmpty(),
                    errorText = stringResource(id = R.string.error_enter_userlastname).takeIf { uiState.isLastNameValid == false },
                    isEmail = false
                )

                LabelTextField(
                    onTextChange = { emailInput ->
                        email = emailInput.trim().lowercase()
                        onEmailInputChanged(email.orEmpty())
                    },
                    label = stringResource(id = sharedR.string.email_text),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    modifier = Modifier
                        .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                        .focusRequester(emailFocusRequester)
                        .focusProperties {
                            next = passwordFocusRequester
                            previous = lastNameFocusRequester
                        }
                        .testTag(CREATE_ACCOUNT_EMAIL_TAG),
                    text = email.orEmpty(),
                    errorText = if (email.isNullOrBlank() && uiState.isEmailValid != null) stringResource(
                        id = R.string.error_enter_email
                    ) else if (uiState.isEmailValid == false) stringResource(
                        id = R.string.error_invalid_email
                    ) else null,
                    isEmail = true
                )

                PasswordTextField(
                    onTextChange = { passwordInput ->
                        password = passwordInput.trim()
                        onPasswordInputChanged(password.orEmpty())
                    },
                    hint = stringResource(id = sharedR.string.password_text),
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(onNext = { confirmPasswordFocusRequester.requestFocus() }),
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            top = 24.dp,
                            end = 16.dp
                        )
                        .focusRequester(passwordFocusRequester)
                        .focusProperties {
                            next = confirmPasswordFocusRequester
                            previous = emailFocusRequester
                        }
                        .testTag(CREATE_ACCOUNT_PASSWORD_TAG),
                    text = password.orEmpty(),
                    errorText = if (uiState.isPasswordValid == false && uiState.passwordStrength == PasswordStrength.INVALID)
                        stringResource(id = R.string.error_enter_password) else if (uiState.isPasswordValid == false && uiState.passwordStrength == PasswordStrength.VERY_WEAK) stringResource(
                        id = R.string.error_password
                    ) else null,
                )

                if (uiState.passwordStrength != PasswordStrength.INVALID && uiState.isPasswordValid != false) {
                    val (passwordStrengthText, passwordAdviceText) = getPasswordStrengthTexts(
                        uiState
                    )
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                        MegaPasswordStrength(
                            passwordStrengthValue = uiState.passwordStrength.value,
                            passwordStrengthText = passwordStrengthText,
                            modifier = Modifier.testTag(CREATE_ACCOUNT_PASSWORD_STRENGTH_VIEW_TAG)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MegaText(
                            text = passwordAdviceText,
                            textColor = TextColor.Primary,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.testTag(
                                CREATE_ACCOUNT_PASSWORD_STRENGTH_ADVICE_TEXT_TAG
                            )
                        )
                    }
                }

                PasswordTextField(
                    onTextChange = {
                        confirmPassword = it
                        onConfirmPasswordInputChanged(it)
                    },
                    hint = stringResource(id = R.string.confirm_password_text),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = {
                        onButtonClicked(
                            action = onCreateAccountClicked,
                            focusManager = focusManager
                        )
                    }),
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp
                        )
                        .focusRequester(confirmPasswordFocusRequester)
                        .focusProperties {
                            next = confirmPasswordFocusRequester
                            previous = passwordFocusRequester
                        }
                        .testTag(CREATE_ACCOUNT_CONFIRM_PASSWORD_TAG),
                    text = confirmPassword.orEmpty(),
                    errorText = if (confirmPassword.isNullOrBlank() && uiState.isConfirmPasswordMatched != null) stringResource(
                        id = mega.privacy.android.shared.resources.R.string.general_confirm_your_password
                    ) else if (uiState.isConfirmPasswordMatched == false)
                        stringResource(id = R.string.error_passwords_dont_match) else null
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 64.dp,
                            end = 32.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MegaCheckbox(
                        modifier = Modifier.testTag(CREATE_ACCOUNT_TERMS_OF_SERVICE_CHECK_BOX_TAG),
                        checked = termsOfServiceAgreed,
                        rounded = false,
                        onCheckedChange = {
                            termsOfServiceAgreed = it
                            onTermsOfServiceAgreedChanged(it)
                        },
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    MegaSpannedText(
                        modifier = Modifier
                            .testTag(CREATE_ACCOUNT_TERMS_OF_SERVICE_TEXT_TAG)
                            .clickable {
                                openTermsAndServiceLink()
                            },
                        value = stringResource(id = R.string.tos),
                        baseStyle = MaterialTheme.typography.caption,
                        styles = mapOf(
                            SpanIndicator('A') to MegaSpanStyle(
                                spanStyle = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                ),
                                color = TextColor.Info
                            )
                        ),
                        color = TextColor.Primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            top = 24.dp,
                            end = 32.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MegaCheckbox(
                        modifier = Modifier.testTag(
                            CREATE_ACCOUNT_END_TO_END_ENCRYPTION_CHECK_BOX_TAG
                        ),
                        checked = e2eeAgreed,
                        rounded = false,
                        onCheckedChange = {
                            e2eeAgreed = it
                            onE2EEAgreedChanged(it)
                        },
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    MegaSpannedText(
                        modifier = Modifier
                            .testTag(CREATE_ACCOUNT_END_TO_END_ENCRYPTION_TEXT_TAG)
                            .clickable {
                                openEndToEndEncryptionLink()
                            },
                        value = stringResource(id = R.string.top),
                        baseStyle = MaterialTheme.typography.caption,
                        styles = mapOf(
                            SpanIndicator('A') to MegaSpanStyle(
                                spanStyle = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                ),
                                color = TextColor.Info
                            ),
                            SpanIndicator('B') to MegaSpanStyle(
                                spanStyle = SpanStyle(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = TextColor.Primary
                            )
                        ),
                        color = TextColor.Primary
                    )
                }

                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 32.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RaisedDefaultMegaButton(
                        modifier = Modifier
                            .testTag(CREATE_ACCOUNT_CREATE_ACCOUNT_BUTTON_TAG),
                        textId = R.string.create_account,
                        onClick = {
                            onButtonClicked(
                                action = onCreateAccountClicked,
                                focusManager = focusManager
                            )
                        },
                        enabled = true
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    TextMegaButton(
                        modifier = Modifier
                            .testTag(CREATE_ACCOUNT_LOGIN_BUTTON_TAG),
                        textId = sharedR.string.login_text,
                        onClick = {
                            onButtonClicked(
                                action = onLoginClicked,
                                focusManager = focusManager
                            )
                        },
                    )
                }
            }

            if (uiState.showNoNetworkWarning && !uiState.isConnected) {
                WarningBanner(
                    modifier = Modifier.testTag(CREATE_ACCOUNT_NETWORK_WARNING_TAG),
                    textString = stringResource(R.string.error_server_connection_problem),
                    onCloseClick = onCloseNetworkWarningClick
                )
            }
        }
    }
}

@Composable
private fun getPasswordStrengthTexts(uiState: CreateAccountUIState): Pair<String, String> {
    var passwordStrengthText = ""
    var passwordAdviceText = ""
    when (uiState.passwordStrength) {
        PasswordStrength.VERY_WEAK -> {
            passwordStrengthText = stringResource(id = R.string.pass_very_weak)
            passwordAdviceText = stringResource(id = R.string.passwd_weak)
        }

        PasswordStrength.WEAK -> {
            passwordStrengthText = stringResource(id = R.string.pass_weak)
            passwordAdviceText = stringResource(id = R.string.passwd_weak)
        }

        PasswordStrength.MEDIUM -> {
            passwordStrengthText = stringResource(id = R.string.pass_medium)
            passwordAdviceText = stringResource(id = R.string.passwd_medium)
        }

        PasswordStrength.GOOD -> {
            passwordStrengthText = stringResource(id = R.string.pass_good)
            passwordAdviceText = stringResource(id = R.string.passwd_good)
        }

        PasswordStrength.STRONG -> {
            passwordStrengthText = stringResource(id = R.string.pass_strong)
            passwordAdviceText = stringResource(id = R.string.passwd_strong)
        }

        else -> {}
    }
    return Pair(passwordStrengthText, passwordAdviceText)
}

private fun onButtonClicked(action: () -> Unit, focusManager: FocusManager) {
    focusManager.clearFocus(true)
    action()
}

@Composable
private fun CreateAccountInProgressView(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = stringResource(id = R.string.login_to_mega),
            modifier = Modifier
                .align(Alignment.Center)
                .size(288.dp)
                .testTag(CREATE_ACCOUNT_IN_PROGRESS_MEGA_LOGO_TAG),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MegaAnimatedLinearProgressIndicator(
                modifier = Modifier
                    .padding(start = 56.dp, end = 56.dp)
                    .testTag(CREATE_ACCOUNT_IN_PROGRESS_PROGRESS_INDICATOR_TAG)
            )
            Spacer(modifier = Modifier.height(12.dp))
            MegaText(
                text = stringResource(id = R.string.create_account_creating_account),
                style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                modifier = Modifier
                    .padding(
                        start = 56.dp, end = 56.dp
                    )
                    .testTag(CREATE_ACCOUNT_IN_PROGRESS_TEXT_TAG),
                textAlign = TextAlign.Center,
                textColor = TextColor.Primary,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenLandingPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.INVALID),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenErrorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(
                isFirstNameValid = false,
                isLastNameValid = false,
                isEmailValid = false,
                isPasswordValid = false,
                isConfirmPasswordMatched = false
            ),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenVeryWeakPasswordPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.VERY_WEAK),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenWeakPasswordPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.WEAK),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenMediumPasswordPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.MEDIUM),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenGoodPasswordPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.GOOD),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}


@CombinedThemePreviews
@Composable
private fun CreateAccountScreenStrongPasswordPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(passwordStrength = PasswordStrength.STRONG),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenNoNetworkPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(
                isConnected = false,
                showNoNetworkWarning = true
            ),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CreateAccountScreenInProgressPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CreateAccountScreen(
            uiState = CreateAccountUIState(
                isLoading = true
            ),
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onEmailInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onCreateAccountClicked = {},
            onTermsOfServiceAgreedChanged = {},
            onE2EEAgreedChanged = {},
            onLoginClicked = {},
            openTermsAndServiceLink = {},
            openEndToEndEncryptionLink = {},
            onResetShowAgreeToTermsEvent = { },
            onCloseNetworkWarningClick = { },
            onResetCreateAccountStatusEvent = { },
            onCreateAccountSuccess = { }
        )
    }
}

internal const val CREATE_ACCOUNT_TOOLBAR_TAG = "create_account_screen:toolbar_title"
internal const val CREATE_ACCOUNT_FIRST_NAME_TAG = "create_account_screen:text_field_first_name"
internal const val CREATE_ACCOUNT_LAST_NAME_TAG = "create_account_screen:text_field_last_name"
internal const val CREATE_ACCOUNT_EMAIL_TAG = "create_account_screen:text_field_email"
internal const val CREATE_ACCOUNT_PASSWORD_TAG = "create_account_screen:text_field_password"
internal const val CREATE_ACCOUNT_CONFIRM_PASSWORD_TAG =
    "create_account_screen:text_field_confirm_password"
internal const val CREATE_ACCOUNT_TERMS_OF_SERVICE_CHECK_BOX_TAG =
    "create_account_screen:check_box_terms_of_service"
internal const val CREATE_ACCOUNT_TERMS_OF_SERVICE_TEXT_TAG = "create_account_screen:text_tos"
internal const val CREATE_ACCOUNT_END_TO_END_ENCRYPTION_CHECK_BOX_TAG =
    "create_account_screen:check_box_e2ee"
internal const val CREATE_ACCOUNT_END_TO_END_ENCRYPTION_TEXT_TAG = "create_account_screen:text_e2ee"
internal const val CREATE_ACCOUNT_CREATE_ACCOUNT_BUTTON_TAG =
    "create_account_screen:button_create_account"
internal const val CREATE_ACCOUNT_LOGIN_BUTTON_TAG = "create_account_screen:button_login"
internal const val CREATE_ACCOUNT_PASSWORD_STRENGTH_VIEW_TAG =
    "create_account_screen:view_password_strength"
internal const val CREATE_ACCOUNT_PASSWORD_STRENGTH_ADVICE_TEXT_TAG =
    "create_account_screen:text_password_strength"
internal const val CREATE_ACCOUNT_NETWORK_WARNING_TAG =
    "create_account_screen:banner_network_warning"
internal const val CREATE_ACCOUNT_IN_PROGRESS_MEGA_LOGO_TAG = "create_account_screen:logo_mega"
internal const val CREATE_ACCOUNT_IN_PROGRESS_PROGRESS_INDICATOR_TAG =
    "create_account_screen:progress_indicator"
internal const val CREATE_ACCOUNT_IN_PROGRESS_TEXT_TAG = "create_account_screen:text_progress"


