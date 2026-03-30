package mega.privacy.android.core.nodecomponents.dialog.openlink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog that allows the user to paste a MEGA link and open it.
 *
 * @param onOpenLink Called when the user confirms with the entered link text
 * @param onDismiss Called when the dialog is dismissed
 * @param modifier Modifier
 */
@Composable
fun OpenLinkDialog(
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("", TextRange(0)))
    }

    BasicInputDialog(
        modifier = modifier.testTag(OPEN_LINK_DIALOG_TAG),
        title = stringResource(id = sharedR.string.open_link_dialog_title),
        placeholder = stringResource(id = sharedR.string.open_link_dialog_hint),
        positiveButtonText = stringResource(id = sharedR.string.general_open_button),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onValueChange = { inputValue = it },
        inputValue = inputValue,
        onPositiveButtonClicked = {
            onOpenLink(inputValue.text)
        },
        onNegativeButtonClicked = onDismiss,
        isAutoShowKeyboard = true,
        onDismiss = onDismiss,
    )
}

internal const val OPEN_LINK_DIALOG_TAG = "open_link_dialog:input_dialog"

@Composable
@CombinedThemePreviews
private fun PreviewOpenLinkDialog() {
    AndroidThemeForPreviews {
        OpenLinkDialog(
            onOpenLink = {},
            onDismiss = {},
        )
    }
}
