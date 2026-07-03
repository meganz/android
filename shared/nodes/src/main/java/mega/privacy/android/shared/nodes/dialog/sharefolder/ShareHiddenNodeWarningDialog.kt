package mega.privacy.android.shared.nodes.dialog.sharefolder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Warning shown before sharing a hidden/sensitive folder with contacts. Sharing a hidden folder
 * makes its contents visible to the recipient, so the user confirms first.
 *
 * Stateless — the caller decides when to show it (only when there is a sensitive folder in the
 * share) and reacts to the callbacks.
 *
 * @param sharingMultipleFolders whether more than one folder is being shared; selects the singular
 * vs plural wording.
 * @param onConfirm invoked when the user chooses to continue with the share.
 * @param onCancel invoked when the user cancels the share.
 */
@Composable
fun ShareHiddenNodeWarningDialog(
    sharingMultipleFolders: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(SHARE_HIDDEN_NODE_WARNING_DIALOG_TAG),
        title = SpannableText(
            stringResource(
                if (sharingMultipleFolders) {
                    sharedR.string.hidden_items
                } else {
                    sharedR.string.hidden_item
                }
            )
        ),
        description = SpannableText(
            stringResource(
                if (sharingMultipleFolders) {
                    sharedR.string.share_hidden_folders_description
                } else {
                    sharedR.string.share_hidden_folder_description
                }
            )
        ),
        positiveButtonText = stringResource(sharedR.string.button_continue),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onCancel,
    )
}

/**
 * PreviewShareHiddenNodeWarningDialogSingleFolder
 */
@CombinedThemePreviews
@Composable
private fun PreviewShareHiddenNodeWarningDialogSingleFolder() {
    AndroidThemeForPreviews {
        ShareHiddenNodeWarningDialog(
            sharingMultipleFolders = false,
            onConfirm = {},
            onCancel = {},
        )
    }
}

/**
 * PreviewShareHiddenNodeWarningDialogMultipleFolders
 */
@CombinedThemePreviews
@Composable
private fun PreviewShareHiddenNodeWarningDialogMultipleFolders() {
    AndroidThemeForPreviews {
        ShareHiddenNodeWarningDialog(
            sharingMultipleFolders = true,
            onConfirm = {},
            onCancel = {},
        )
    }
}

internal const val SHARE_HIDDEN_NODE_WARNING_DIALOG_TAG = "share_hidden_node_warning:dialog"
