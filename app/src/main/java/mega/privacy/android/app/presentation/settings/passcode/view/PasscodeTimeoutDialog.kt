package mega.privacy.android.app.presentation.settings.passcode.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.passcode.model.PasscodeTimeoutUIState
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun PasscodeTimeoutDialog(
    state: PasscodeTimeoutUIState,
    onTimeoutSelected: (TimeoutOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ConfirmationDialogWithRadioButtons(
        modifier = Modifier.testTag(PASSCODE_TIMEOUT_DIALOG),
        titleText = stringResource(R.string.settings_require_passcode),
        initialSelectedOption = state.currentOption,
        radioOptions = state.options,
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onOptionSelected = onTimeoutSelected,
        onDismissRequest = onDismiss,
        optionDescriptionMapper = { it.getTitle(context) },
    )
}

@CombinedThemePreviews
@Composable
private fun PasscodeTimeoutDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var state by remember {
            mutableStateOf(
                PasscodeTimeoutUIState(
                    options = persistentListOf(
                        TimeoutOption.Immediate,
                        TimeoutOption.SecondsTimeSpan(5),
                        TimeoutOption.MinutesTimeSpan(2),
                        TimeoutOption.MinutesTimeSpan(5),
                    ),
                    currentOption = null
                )
            )
        }

        PasscodeTimeoutDialog(
            state = state,
            onTimeoutSelected = {
                state = state.copy(currentOption = it)
            },
            onDismiss = {}
        )
    }
}


internal const val PASSCODE_TIMEOUT_DIALOG =
    "passcode_timeout_dialog:confirmation_dialog_with_radio_buttons"