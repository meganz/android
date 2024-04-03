package mega.privacy.android.app.presentation.meeting.managechathistory.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
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
 * @param selectedOption The current selected option for a specific chat room
 * @param onOptionSelected An event that requests the selected retention time option to change
 * @param isConfirmButtonEnable Decides the enablement of the confirm button
 * @param onDismissRequest The callback that will be triggered when the user requests to dismiss the dialog
 * @param onConfirmClick The callback that will be triggered when the user clicks the confirm button (either the 'Next' or 'OK' button)
 * @param modifier The [Modifier] for this [Composable]
 */
@Composable
internal fun ChatHistoryRetentionConfirmationDialog(
    selectedOption: ChatHistoryRetentionOption,
    onOptionSelected: (option: ChatHistoryRetentionOption) -> Unit,
    confirmButtonText: String,
    isConfirmButtonEnable: () -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirmClick: (option: ChatHistoryRetentionOption) -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialogWithRadioButtons(
    modifier = modifier,
    titleText = stringResource(id = R.string.title_properties_history_retention),
    subTitleText = stringResource(id = R.string.subtitle_properties_manage_chat),
    radioOptions = ChatHistoryRetentionOption.entries,
    initialSelectedOption = selectedOption,
    optionDescriptionMapper = @Composable {
        stringResource(id = it.stringId)
    },
    onOptionSelected = onOptionSelected,
    confirmButtonText = confirmButtonText,
    isConfirmButtonEnable = isConfirmButtonEnable,
    onConfirmRequest = onConfirmClick,
    cancelButtonText = stringResource(id = R.string.general_cancel),
    onDismissRequest = onDismissRequest,
)

@CombinedThemePreviews
@Composable
private fun ChatHistoryRetentionOptionsDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatHistoryRetentionConfirmationDialog(
            selectedOption = ChatHistoryRetentionOption.Disabled,
            onOptionSelected = {},
            confirmButtonText = "OK",
            isConfirmButtonEnable = { true },
            onConfirmClick = {},
            onDismissRequest = {}
        )
    }
}
