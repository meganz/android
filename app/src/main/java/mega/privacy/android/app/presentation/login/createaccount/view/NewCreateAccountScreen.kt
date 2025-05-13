package mega.privacy.android.app.presentation.login.createaccount.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.inputfields.HelpTextError
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.EMAIL_CHAR_LIMIT
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_CONFIRM_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_EMAIL
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_LAST_NAME
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel.Companion.KEY_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.CONFIRM_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.CREATE_ACCOUNT_BUTTON
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.LAST_NAME
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.PASSWORD_HINT
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.TERMS_CONDITION_CHECKBOX
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.TOOLBAR
import mega.privacy.android.app.presentation.login.view.tabletScreenWidth
import mega.privacy.android.app.utils.Constants.TERMS_OF_SERVICE_URL
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NewCreateAccountRoute(
    uiState: CreateAccountUIState,
    onNavigateToLogin: () -> Unit,
    openLink: (String) -> Unit,
    setTemporalDataForAccountCreation: (EphemeralCredentials) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateAccountViewModel = hiltViewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    NewCreateAccountScreen(
        uiState = uiState,
        snackBarHostState = snackBarHostState,
        onFirstNameInputChanged = viewModel::onFirstNameInputChanged,
        onLastNameInputChanged = viewModel::onLastNameInputChanged,
        onEmailInputChanged = viewModel::onEmailInputChanged,
        onPasswordInputChanged = viewModel::onPasswordInputChanged,
        onConfirmPasswordInputChanged = viewModel::onConfirmPasswordInputChanged,
        onCreateAccountClicked = viewModel::createAccount,
        onTermsOfServiceAgreedChanged = viewModel::termsOfServiceAgreedChanged,
        onLoginClicked = onNavigateToLogin,
        openLink = openLink,
        onResetCreateAccountStatusEvent = viewModel::resetCreateAccountStatusEvent,
        onResetShowAgreeToTermsEvent = viewModel::resetShowAgreeToTermsEvent,
        onCreateAccountSuccess = {
            setTemporalDataForAccountCreation(it)
            viewModel.onCreateAccountSuccess(it)
        },
        onBackIconPressed = onNavigateToLogin,
        onNetworkWarningShown = viewModel::networkWarningShown,
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NewCreateAccountScreen(
    uiState: CreateAccountUIState,
    snackBarHostState: SnackbarHostState,
    onFirstNameInputChanged: (String) -> Unit,
    onLastNameInputChanged: (String) -> Unit,
    onEmailInputChanged: (String) -> Unit,
    onPasswordInputChanged: (String) -> Unit,
    onConfirmPasswordInputChanged: (String) -> Unit,
    onCreateAccountClicked: () -> Unit,
    onTermsOfServiceAgreedChanged: (Boolean) -> Unit,
    onLoginClicked: () -> Unit,
    openLink: (String) -> Unit,
    onResetCreateAccountStatusEvent: () -> Unit,
    onResetShowAgreeToTermsEvent: () -> Unit,
    onCreateAccountSuccess: (EphemeralCredentials) -> Unit,
    onBackIconPressed: () -> Unit,
    onNetworkWarningShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    val orientation = LocalConfiguration.current.orientation
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordFocus by rememberSaveable { mutableStateOf(false) }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var termsCheckState by rememberSaveable { mutableStateOf(false) }
    var showAgreeToTerms by rememberSaveable { mutableStateOf(false) }
    var passwordHintHeight by rememberSaveable { mutableFloatStateOf(0F) }
    var onSignUpButtonClicked by rememberSaveable { mutableStateOf(false) }
    val fieldOffsets = remember { mutableMapOf<String, Int>() }
    val isMinimumCharacterError = uiState.isPasswordLengthSufficient == false
    val isWeakPassword =
        uiState.passwordStrength == PasswordStrength.WEAK || uiState.passwordStrength == PasswordStrength.VERY_WEAK
    var showAccountExistsMessage by rememberSaveable { mutableStateOf(false) }
    val softKeyboard = LocalSoftwareKeyboardController.current

    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet

    EventEffect(
        event = uiState.showAgreeToTermsEvent,
        onConsumed = onResetShowAgreeToTermsEvent
    ) {
        showAgreeToTerms = true
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
                showAccountExistsMessage = true
            }

            is CreateAccountStatus.UnknownError -> {
                snackBarHostState.showSnackbar(
                    message = createAccountStatus.message
                )
            }
        }
    }

    val noNetworkMessage = stringResource(R.string.error_server_connection_problem)
    LaunchedEffect(uiState.showNoNetworkWarning, uiState.isConnected) {
        if (uiState.showNoNetworkWarning && !uiState.isConnected) {
            snackBarHostState.showSnackbar(
                message = noNetworkMessage
            )
            onNetworkWarningShown()
        }
    }

    if (isPhoneLandscape) {
        LaunchedEffect(uiState, showAccountExistsMessage, onSignUpButtonClicked) {
            val offset: Int? = when {
                uiState.isFirstNameValid == false -> fieldOffsets[KEY_FIRST_NAME]
                uiState.isLastNameValid == false -> fieldOffsets[KEY_LAST_NAME]
                uiState.isEmailValid == false || uiState.isEmailLengthExceeded == true || showAccountExistsMessage
                    -> fieldOffsets[KEY_EMAIL]

                uiState.isPasswordValid == false -> fieldOffsets[KEY_PASSWORD]
                uiState.isConfirmPasswordMatched == false -> fieldOffsets[KEY_CONFIRM_PASSWORD]
                else -> null
            }
            offset?.let {
                if (onSignUpButtonClicked) {
                    Timber.d("Scroll to offset: $it")
                    scrollState.animateScrollTo(offset)
                    onSignUpButtonClicked = false
                }
            }
        }
    }

    BackHandler(enabled = true) {
        onBackIconPressed()
    }

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .semantics { testTagsAsResourceId = true },
        snackbarHost = {
            MegaSnackbar(
                snackBarHostState = snackBarHostState,
            )
        },
        topBar = {
            AnimatedVisibility(
                visible = !scrollState.canScrollBackward || !isPhoneLandscape,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
            ) {
                MegaTopAppBar(
                    modifier = Modifier
                        .testTag(TOOLBAR)
                        .statusBarsPadding(),
                    title = stringResource(id = R.string.create_account_title),
                    navigationType = AppBarNavigationType.Back(
                        onNavigationIconClicked = onBackIconPressed,
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
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
            } else {
                Modifier.fillMaxSize()
            }

            Column(
                modifier = contentModifier
            ) {
                TextInputField(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            fieldOffsets[KEY_FIRST_NAME] = coordinates.positionInParent().y.toInt()
                        }
                        .testTag(FIRST_NAME)
                        .focusRequester(firstNameFocusRequester)
                        .focusProperties {
                            next = lastNameFocusRequester
                            previous = firstNameFocusRequester
                        }
                        .padding(
                            top = LocalSpacing.current.x24,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        ),
                    label = stringResource(id = R.string.first_name_text),
                    keyboardType = KeyboardType.Text,
                    onValueChanged = {
                        firstName = it.trim()
                        onFirstNameInputChanged(firstName)
                    },
                    imeAction = ImeAction.Next,
                    errorText = stringResource(id = sharedR.string.sign_up_first_name_text_field_error_message)
                        .takeIf { uiState.isFirstNameValid == false },
                    text = firstName
                )

                TextInputField(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            fieldOffsets[KEY_LAST_NAME] = coordinates.positionInParent().y.toInt()
                        }
                        .testTag(LAST_NAME)
                        .focusRequester(lastNameFocusRequester)
                        .focusProperties {
                            next = emailFocusRequester
                            previous = firstNameFocusRequester
                        }
                        .padding(
                            top = LocalSpacing.current.x16,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        ),
                    label = stringResource(id = R.string.lastname_text),
                    keyboardType = KeyboardType.Text,
                    onValueChanged = {
                        lastName = it.trim()
                        onLastNameInputChanged(lastName)
                    },
                    imeAction = ImeAction.Next,
                    errorText = stringResource(id = sharedR.string.sign_up_last_name_text_field_error_message)
                        .takeIf { uiState.isLastNameValid == false },
                    text = lastName
                )

                TextInputField(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            fieldOffsets[KEY_EMAIL] = coordinates.positionInParent().y.toInt()
                        }
                        .testTag(CreateAccountTestTags.EMAIL)
                        .focusRequester(emailFocusRequester)
                        .focusProperties {
                            next = passwordFocusRequester
                            previous = lastNameFocusRequester
                        }
                        .padding(
                            top = LocalSpacing.current.x16,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        ),
                    label = stringResource(id = sharedR.string.email_text),
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    onValueChanged = {
                        email = it.trim().lowercase()
                        showAccountExistsMessage = false
                        onEmailInputChanged(email)
                    },
                    imeAction = ImeAction.Next,
                    errorText = when {
                        showAccountExistsMessage -> stringResource(id = R.string.error_email_registered)
                        uiState.isEmailLengthExceeded == true -> stringResource(id = sharedR.string.sign_up_email_text_field_char_limit_exceed_error)
                        uiState.isEmailValid == false -> stringResource(id = sharedR.string.login_invalid_email_error_message)
                        else -> null
                    },
                    text = email
                )

                PasswordTextInputField(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            fieldOffsets[KEY_PASSWORD] = coordinates.positionInParent().y.toInt()
                        }
                        .testTag(PASSWORD)
                        .focusRequester(passwordFocusRequester)
                        .focusProperties {
                            next = confirmPasswordFocusRequester
                            previous = emailFocusRequester
                        }
                        .padding(
                            top = LocalSpacing.current.x16,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        ),
                    label = stringResource(id = sharedR.string.password_text),
                    onValueChanged = {
                        password = it
                        onPasswordInputChanged(it)

                    },
                    imeAction = ImeAction.Next,
                    onFocusChanged = { isPasswordFocus = it },
                    errorText = when {
                        !isPasswordFocus && (isMinimumCharacterError || isWeakPassword) -> ""
                        else -> null
                    },
                    text = password
                )

                PasswordHint(
                    modifier = Modifier
                        .testTag(PASSWORD_HINT)
                        .padding(
                            top = LocalSpacing.current.x4,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        )
                        .onGloballyPositioned { coordinate ->
                            passwordHintHeight = coordinate.size.height.toFloat()
                        },
                    isVisible = isPasswordFocus
                            || password.isNullOrBlank().not()
                            || isMinimumCharacterError
                            || isWeakPassword,
                    isMinimumCharacterError = isMinimumCharacterError,
                    passwordStrength = uiState.passwordStrength,
                    doesContainMixedCase = uiState.doesPasswordContainMixedCase,
                    doesContainNumberOrSpecialCharacter = uiState.doesPasswordContainNumeric || uiState.doesPasswordContainSpecialCharacter
                )

                PasswordTextInputField(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            fieldOffsets[KEY_CONFIRM_PASSWORD] =
                                coordinates.positionInParent().y.toInt()
                        }
                        .testTag(CONFIRM_PASSWORD)
                        .focusRequester(confirmPasswordFocusRequester)
                        .focusProperties {
                            next = confirmPasswordFocusRequester
                            previous = passwordFocusRequester
                        }
                        .padding(
                            top = LocalSpacing.current.x16,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        ),
                    label = stringResource(id = R.string.confirm_password_text),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = {
                        onButtonClicked(
                            action = onCreateAccountClicked,
                            focusManager = focusManager
                        )
                    }),
                    onValueChanged = {
                        confirmPassword = it
                        onConfirmPasswordInputChanged(it)
                    },
                    errorText = when {
                        confirmPassword.isEmpty() && uiState.isConfirmPasswordMatched == false -> stringResource(
                            id = R.string.confirm_password_text
                        )

                        uiState.isConfirmPasswordMatched == false -> stringResource(id = sharedR.string.sign_up_confirm_password_not_match_error_message)
                        else -> null
                    },
                    text = confirmPassword
                )

                Row(
                    modifier = Modifier.padding(
                        top = LocalSpacing.current.x24,
                        start = LocalSpacing.current.x4
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.testTag(TERMS_CONDITION_CHECKBOX),
                        checked = termsCheckState,
                        onCheckStateChanged = {
                            termsCheckState = it
                            showAgreeToTerms = false
                            onTermsOfServiceAgreedChanged(it)
                        }
                    )

                    LinkSpannedText(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(Alignment.CenterVertically),
                        value = stringResource(id = sharedR.string.sign_up_terms_condition_content),
                        spanStyles = hashMapOf(
                            SpanIndicator('A') to SpanStyleWithAnnotation(
                                MegaSpanStyle.LinkColorStyle(
                                    SpanStyle(),
                                    LinkColor.Primary
                                ),
                                stringResource(id = sharedR.string.sign_up_terms_condition_content)
                                    .substringAfter("[A]")
                                    .substringBefore("[/A]")
                            )
                        ),
                        onAnnotationClick = {
                            openLink(TERMS_OF_SERVICE_URL)
                        },
                        baseStyle = AppTheme.typography.bodySmall,
                    )
                }

                if (showAgreeToTerms) {
                    HelpTextError(
                        modifier = Modifier
                            .testTag("$TERMS_CONDITION_CHECKBOX:error")
                            .padding(
                                start = LocalSpacing.current.x16,
                                top = LocalSpacing.current.x4
                            )
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.create_account_no_terms)
                    )
                }

                PrimaryFilledButton(
                    modifier = Modifier
                        .testTag(CREATE_ACCOUNT_BUTTON)
                        .fillMaxWidth()
                        .padding(
                            top = LocalSpacing.current.x24,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        )
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                        softKeyboard?.hide()
                        onCreateAccountClicked()
                        onSignUpButtonClicked = true
                    },
                    text = stringResource(id = R.string.create_account),
                    isLoading = uiState.isLoading
                )

                LinkSpannedText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            top = LocalSpacing.current.x24,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16,
                            bottom = LocalSpacing.current.x56
                        ),
                    value = stringResource(id = sharedR.string.sign_up_already_have_account_login_link_text),
                    spanStyles = hashMapOf(
                        SpanIndicator('A') to SpanStyleWithAnnotation(
                            MegaSpanStyle.LinkColorStyle(
                                SpanStyle(),
                                LinkColor.Primary
                            ),
                            stringResource(id = sharedR.string.sign_up_already_have_account_login_link_text)
                                .substringAfter("[A]")
                                .substringBefore("[/A]")
                        )
                    ),
                    onAnnotationClick = {
                        onLoginClicked()
                    },
                    baseStyle = AppTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                )
            }
        }
    }
}

private fun onButtonClicked(action: () -> Unit, focusManager: FocusManager) {
    focusManager.clearFocus(true)
    action()
}

internal object CreateAccountTestTags {
    private const val CREATE_ACCOUNT_SCREEN = "create_account_screen"
    const val TOOLBAR = "$CREATE_ACCOUNT_SCREEN:toolbar"
    const val FIRST_NAME = "$CREATE_ACCOUNT_SCREEN:first_name:text_input"
    const val LAST_NAME = "$CREATE_ACCOUNT_SCREEN:last_name:text_input"
    const val EMAIL = "$CREATE_ACCOUNT_SCREEN:email:text_input"
    const val PASSWORD = "$CREATE_ACCOUNT_SCREEN:password:text_input"
    const val PASSWORD_HINT = "$CREATE_ACCOUNT_SCREEN:password_hint"
    const val CONFIRM_PASSWORD = "$CREATE_ACCOUNT_SCREEN:confirm_password:text_input"
    const val TERMS_CONDITION_CHECKBOX = "$CREATE_ACCOUNT_SCREEN:terms_condition:checkbox"
    const val CREATE_ACCOUNT_BUTTON = "$CREATE_ACCOUNT_SCREEN:create_account:button"
}

@CombinedThemePreviewsTablet
@CombinedThemePreviews
@Composable
private fun NewCreateAccountScreenPreview() {
    AndroidThemeForPreviews {
        NewCreateAccountScreen(
            uiState = CreateAccountUIState(),
            snackBarHostState = remember { SnackbarHostState() },
            onBackIconPressed = {},
            onFirstNameInputChanged = {},
            onLastNameInputChanged = {},
            onPasswordInputChanged = {},
            onConfirmPasswordInputChanged = {},
            onEmailInputChanged = {},
            onTermsOfServiceAgreedChanged = {},
            onCreateAccountClicked = {},
            onLoginClicked = {},
            openLink = {},
            onResetCreateAccountStatusEvent = {},
            onResetShowAgreeToTermsEvent = {},
            onNetworkWarningShown = {},
            onCreateAccountSuccess = {},
        )
    }
}