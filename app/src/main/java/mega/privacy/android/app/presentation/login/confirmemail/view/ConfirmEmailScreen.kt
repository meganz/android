package mega.privacy.android.app.presentation.login.confirmemail.view

import mega.privacy.android.shared.resources.R as SharedResR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmEmailViewModel
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.LabelTextField
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ConfirmEmailRoute(
    email: String,
    fullName: String?,
    onShowPendingFragment: (loginFragmentType: LoginFragmentType) -> Unit,
    onSetTemporalEmail: (email: String) -> Unit,
    onCancelConfirmationAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmEmailViewModel = hiltViewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isPendingToShowFragment) {
        uiState.isPendingToShowFragment?.let {
            onShowPendingFragment(it)
            viewModel.isPendingToShowFragmentConsumed()
        }
    }

    LaunchedEffect(uiState.registeredEmail) {
        uiState.registeredEmail?.let {
            onSetTemporalEmail(it)
        }
    }

    val successMessage = stringResource(id = R.string.confirm_email_misspelled_email_sent)
    LaunchedEffect(uiState.shouldShowSuccessMessage) {
        if (uiState.shouldShowSuccessMessage) {
            snackBarHostState.showAutoDurationSnackbar(
                message = successMessage
            )
            viewModel.onSuccessMessageDisplayed()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackBarHostState.showAutoDurationSnackbar(
                message = it
            )
            viewModel.onErrorMessageDisplayed()
        }
    }

    Box(modifier = modifier.semantics { testTagsAsResourceId = true }) {
        val offlineSnackBarMessage = stringResource(id = R.string.error_server_connection_problem)
        ConfirmEmailScreen(
            modifier = Modifier.fillMaxSize(),
            email = email,
            uiState = uiState,
            onCancelClick = {
                viewModel.cancelCreateAccount()
                onCancelConfirmationAccount()
            },
            onResendSignUpLink = {
                viewModel.resendSignUpLink(email = it, fullName = fullName)
            },
            onShowOfflineMessage = {
                coroutineScope.launch {
                    snackBarHostState.showAutoDurationSnackbar(
                        message = offlineSnackBarMessage
                    )
                }
            }
        )

        SnackbarHost(
            modifier = Modifier.align(Alignment.BottomCenter),
            hostState = snackBarHostState
        )
    }
}

@Composable
internal fun ConfirmEmailScreen(
    email: String,
    uiState: ConfirmEmailUiState,
    onCancelClick: () -> Unit,
    onResendSignUpLink: (email: String) -> Unit,
    onShowOfflineMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var emailValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = email,
                selection = TextRange(email.length)
            )
        )
    }
    var errorTextId by remember { mutableStateOf<Int?>(null) }

    val localKeyboardController = LocalSoftwareKeyboardController.current

    Scaffold(modifier = modifier) {
        Column(modifier = Modifier.padding(it)) {
            MegaText(
                modifier = Modifier
                    .padding(top = 15.dp, start = 24.dp, end = 24.dp)
                    .testTag(
                        TITLE_TAG
                    ),
                text = stringResource(id = R.string.confirm_email_text),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1
            )

            Spacer(modifier = Modifier.height(44.dp))

            Icon(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .testTag(ICON_TAG),
                painter = painterResource(id = R.drawable.ic_awaiting_email),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(8.dp))

            MegaText(
                modifier = Modifier.padding(horizontal = 24.dp).testTag(
                    DESCRIPTION_TAG),
                text = stringResource(id = R.string.confirm_email_explanation),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body1
            )

            LabelTextField(
                modifier = Modifier.padding(start = 24.dp, top = 32.dp, end = 24.dp).testTag(
                    EMAIL_ADDRESS_TAG),
                onTextChange = { textValue ->
                    emailValueState = textValue.copy(
                        selection = TextRange(textValue.text.length)
                    )
                    errorTextId = null
                },
                label = stringResource(id = R.string.email_text),
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(),
                value = emailValueState,
                errorText = errorTextId?.let { errorId -> stringResource(id = errorId) },
                isEmail = true,
                isAutoFocus = true
            )

            Spacer(modifier = Modifier.padding(top = 24.dp))

            MegaSpannedText(
                modifier = Modifier.padding(horizontal = 24.dp).testTag(
                    EMAIL_ADDRESS_DESCRIPTION_TAG),
                value = stringResource(id = SharedResR.string.account_confirm_email_misspelled_email_description),
                baseStyle = MaterialTheme.typography.subtitle2,
                styles = mapOf(
                    SpanIndicator('A') to MegaSpanStyle(
                        spanStyle = SpanStyle(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextColor.Secondary
                    )
                ),
                color = TextColor.Secondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 42.dp, start = 24.dp)
            ) {
                RaisedDefaultMegaButton(
                    modifier = Modifier.testTag(RESEND_BUTTON_TAG),
                    textId = R.string.confirm_email_misspelled_resend,
                    onClick = {
                        when {
                            emailValueState.text.isBlank() -> {
                                errorTextId = R.string.error_enter_email
                            }

                            !EMAIL_ADDRESS.matcher(emailValueState.text).matches() -> {
                                errorTextId = R.string.error_invalid_email
                            }

                            else -> {
                                localKeyboardController?.hide()

                                if (uiState.isOnline) {
                                    onResendSignUpLink(
                                        emailValueState
                                            .text
                                            .lowercase()
                                            .trim { char -> char <= ' ' }
                                    )
                                } else {
                                    onShowOfflineMessage()
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(23.dp))

                TextMegaButton(
                    modifier = Modifier.testTag(CANCEL_BUTTON_TAG),
                    textId = R.string.general_cancel,
                    onClick = onCancelClick
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ConfirmEmailScreen(
            modifier = Modifier.fillMaxSize(),
            email = "email@email.com",
            uiState = ConfirmEmailUiState(),
            onCancelClick = {},
            onResendSignUpLink = {},
            onShowOfflineMessage = {}
        )
    }
}

internal const val TITLE_TAG = "confirm_email_screen:text_confirm_email"
internal const val ICON_TAG = "confirm_email_screen:icon_confirm_email"
internal const val DESCRIPTION_TAG =
    "confirm_email_screen:text_confirm_email_description"
internal const val EMAIL_ADDRESS_TAG =
    "confirm_email_screen:text_email_address"
internal const val EMAIL_ADDRESS_DESCRIPTION_TAG =
    "confirm_email_screen:text_email_address_description"
internal const val CANCEL_BUTTON_TAG = "confirm_email_screen:button_cancel"
internal const val RESEND_BUTTON_TAG = "confirm_email_screen:button_resend"
