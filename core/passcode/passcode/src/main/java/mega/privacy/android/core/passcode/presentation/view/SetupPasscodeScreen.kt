package mega.privacy.android.core.passcode.presentation.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.consumed
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.passcode.components.PasscodeField
import mega.privacy.android.core.passcode.presentation.model.PasscodeEntryStage
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.PasscodeSettingScreenEvent

/**
 * Screen for setting a new passcode or changing the existing one.
 *
 * @param uiState
 * @param isEdit true when changing an existing passcode, false when setting one for the first time
 * @param onPasscodeEntered invoked with a complete passcode, on either entry round
 * @param onPasscodeTypeSelected invoked when the user picks a different passcode format
 * @param onNavigateBack
 * @param modifier
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SetupPasscodeScreen(
    uiState: SetupPasscodeUiState,
    isEdit: Boolean,
    onPasscodeEntered: (String) -> Unit,
    onPasscodeTypeSelected: (PasscodeTypeOption) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(PasscodeSettingScreenEvent)
    }

    val state = uiState as? SetupPasscodeUiState.Data ?: return
    // Retained rather than saved: the entry round and the selected type live in the ViewModel, so a
    // flag that outlived it would reopen the sheet over a screen that had reset underneath it.
    var showTypeChooser by retain { mutableStateOf(false) }

    MegaScaffold(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .imePadding(),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(SETUP_PASSCODE_TOP_APP_BAR_TAG),
                title = stringResource(
                    id = if (isEdit) R.string.title_change_passcode
                    else R.string.settings_passcode_lock
                ),
                navigationType = AppBarNavigationType.Back(onNavigateBack),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            MegaText(
                modifier = Modifier.testTag(SETUP_PASSCODE_TITLE_TAG),
                text = stringResource(id = titleResource(state.stage, isEdit)),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                textColor = TextColor.Primary,
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Discards the field's buffer whenever the format or the round changes, so nothing
            // typed against the previous one can leak into the next.
            key(state.passcodeType, state.stage::class) {
                PasscodeInput(
                    passcodeType = state.passcodeType,
                    onPasscodeEntered = onPasscodeEntered,
                )
            }

            if (state.stage is PasscodeEntryStage.Confirmation && state.stage.showMismatchError) {
                Spacer(modifier = Modifier.height(20.dp))
                MegaText(
                    modifier = Modifier.testTag(SETUP_PASSCODE_MISMATCH_ERROR_TAG),
                    text = stringResource(id = R.string.pin_lock_not_match),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Error,
                )
            }

            if (state.stage is PasscodeEntryStage.FirstEntry) {
                Spacer(modifier = Modifier.height(20.dp))
                TextOnlyButton(
                    modifier = Modifier.testTag(SETUP_PASSCODE_OPTIONS_BUTTON_TAG),
                    text = stringResource(id = R.string.settings_passcode_option),
                    onClick = { showTypeChooser = true },
                )
            }
        }

        if (showTypeChooser) {
            PasscodeTypeChooserBottomSheet(
                selectedOption = state.passcodeType.toOption(),
                onOptionSelected = {
                    showTypeChooser = false
                    onPasscodeTypeSelected(it)
                },
                onDismissRequest = { showTypeChooser = false },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasscodeInput(
    passcodeType: PasscodeUIType,
    onPasscodeEntered: (String) -> Unit,
) {
    when (passcodeType) {
        is PasscodeUIType.Pin -> PasscodeField(
            onComplete = onPasscodeEntered,
            modifier = Modifier
                .testTag(SETUP_PASSCODE_FIELD_TAG)
                .semantics(
                    mergeDescendants = true,
                    properties = {
                        // Accessibility services deliver the whole passcode in one action rather
                        // than a character at a time, so the field's own length gate never runs and
                        // has to be applied here.
                        setText { text ->
                            val passcode = text.text
                            if (passcode.length != passcodeType.digits) return@setText false
                            onPasscodeEntered(passcode)
                            true
                        }
                    }
                ),
            numberOfCharacters = passcodeType.digits,
        )

        is PasscodeUIType.Alphanumeric -> PasswordField(onPasscodeEntered = onPasscodeEntered)
    }
}

@Composable
private fun PasswordField(onPasscodeEntered: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    PasswordTextInputField(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .testTag(SETUP_PASSCODE_PASSWORD_FIELD_TAG)
            .focusRequester(focusRequester),
        onValueChanged = { password = it },
        text = password,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(
            onDone = {
                // Clearing here mirrors PasscodeField, so a repeated mismatch always starts from
                // an empty field rather than relying on the composable being torn down.
                onPasscodeEntered(password)
                password = ""
            }
        ),
        label = null,
    )
}

private fun titleResource(stage: PasscodeEntryStage, isEdit: Boolean) = when {
    stage is PasscodeEntryStage.FirstEntry && isEdit -> R.string.reset_pin_title
    stage is PasscodeEntryStage.FirstEntry -> R.string.unlock_pin_title
    isEdit -> R.string.reset_pin_title_2
    else -> R.string.unlock_pin_title_2
}

private fun PasscodeUIType.toOption() = when {
    this is PasscodeUIType.Pin && digits == SIX_DIGITS -> PasscodeTypeOption.SixDigits
    this is PasscodeUIType.Pin -> PasscodeTypeOption.FourDigits
    else -> PasscodeTypeOption.Alphanumeric
}

private const val SIX_DIGITS = 6

internal const val SETUP_PASSCODE_TOP_APP_BAR_TAG = "setup_passcode_screen:top_app_bar"
internal const val SETUP_PASSCODE_TITLE_TAG = "setup_passcode_screen:text_title"
internal const val SETUP_PASSCODE_FIELD_TAG = "setup_passcode_screen:passcode_field"
internal const val SETUP_PASSCODE_PASSWORD_FIELD_TAG = "setup_passcode_screen:password_field"
internal const val SETUP_PASSCODE_MISMATCH_ERROR_TAG = "setup_passcode_screen:text_mismatch_error"
internal const val SETUP_PASSCODE_OPTIONS_BUTTON_TAG = "setup_passcode_screen:button_passcode_options"

@CombinedThemePreviews
@Composable
private fun SetupPasscodeScreenPreview(
    @PreviewParameter(SetupPasscodePreviewProvider::class) parameters: SetupPasscodePreviewParameters,
) {
    AndroidThemeForPreviews {
        SetupPasscodeScreen(
            uiState = SetupPasscodeUiState.Data(
                passcodeType = parameters.passcodeType,
                stage = parameters.stage,
                passcodeSavedEvent = consumed,
            ),
            isEdit = parameters.isEdit,
            onPasscodeEntered = {},
            onPasscodeTypeSelected = {},
            onNavigateBack = {},
        )
    }
}

internal data class SetupPasscodePreviewParameters(
    val passcodeType: PasscodeUIType = PasscodeUIType.Pin(false, 4),
    val stage: PasscodeEntryStage = PasscodeEntryStage.FirstEntry,
    val isEdit: Boolean = false,
)

private class SetupPasscodePreviewProvider :
    PreviewParameterProvider<SetupPasscodePreviewParameters> {
    override val values = sequenceOf(
        SetupPasscodePreviewParameters(),
        SetupPasscodePreviewParameters(passcodeType = PasscodeUIType.Pin(false, 6)),
        SetupPasscodePreviewParameters(passcodeType = PasscodeUIType.Alphanumeric(false)),
        SetupPasscodePreviewParameters(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = false)
        ),
        SetupPasscodePreviewParameters(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = true)
        ),
        SetupPasscodePreviewParameters(isEdit = true),
        SetupPasscodePreviewParameters(
            stage = PasscodeEntryStage.Confirmation("1234", showMismatchError = false),
            isEdit = true,
        ),
    )
}
