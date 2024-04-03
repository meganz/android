package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * A [Composable] to display the chat history retention time options in a dialog
 *
 * @param currentOption The current selected option for a specific chat room
 * @param onDismissRequest The callback that will be triggered when the user requests to dismiss the dialog
 * @param onConfirmClick The callback that will be triggered when the user clicks the confirm button (either the 'Next' or 'OK' button)
 * @param modifier The [Modifier] for this [Composable]
 */
@Composable
internal fun ChatHistoryRetentionConfirmationDialog(
    currentOption: ChatHistoryRetentionOption,
    onDismissRequest: () -> Unit,
    onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by rememberSaveable { mutableStateOf(currentOption) }
    val isCustomOptionSelected by remember {
        derivedStateOf {
            selectedOption == ChatHistoryRetentionOption.Custom
        }
    }

    ConfirmationDialogWithRadioButtons(
        modifier = modifier,
        titleText = stringResource(id = R.string.title_properties_history_retention),
        subTitleText = stringResource(id = R.string.subtitle_properties_manage_chat),
        radioOptions = ChatHistoryRetentionOption.entries,
        initialSelectedOption = currentOption,
        optionDescriptionMapper = @Composable {
            stringResource(id = it.stringId)
        },
        onOptionSelected = {
            selectedOption = it
        },
        confirmButtonText = if (isCustomOptionSelected) {
            stringResource(id = R.string.general_next)
        } else {
            stringResource(id = R.string.general_ok)
        },
        isConfirmButtonEnable = {
            selectedOption != ChatHistoryRetentionOption.Disabled
        },
        onConfirmRequest = onConfirmClick,
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onDismissRequest = onDismissRequest,
    )
}

@CombinedThemePreviews
@Composable
private fun ChatHistoryRetentionOptionsDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatHistoryRetentionConfirmationDialog(
            currentOption = ChatHistoryRetentionOption.Disabled,
            onConfirmClick = {},
            onDismissRequest = {}
        )
    }
}
