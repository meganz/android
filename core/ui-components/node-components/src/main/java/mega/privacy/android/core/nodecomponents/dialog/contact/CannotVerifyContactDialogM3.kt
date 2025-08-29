package mega.privacy.android.core.nodecomponents.dialog.contact

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedResR

internal const val CANNOT_VERIFY_DIALOG_TAG = "cannot_verify_dialog"

/**
 * Dialog to show when the user tries to verify a contact that is not in the contact list
 *
 * @param email Email of the contact to verify
 * @param onDismiss dismiss callback of dialog
 */
@Composable
fun CannotVerifyContactDialogM3(
    email: String,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(CANNOT_VERIFY_DIALOG_TAG),
        title = stringResource(id = sharedResR.string.shared_items_contact_not_in_contact_list_dialog_title),
        description = stringResource(
            id = sharedResR.string.shared_items_contact_not_in_contact_list_dialog_content,
            email
        ),
        positiveButtonText = stringResource(id = sharedResR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewCannotVerifyContactDialog() {
    AndroidThemeForPreviews {
        CannotVerifyContactDialogM3(email = "asd@mega.nz", {})
    }
}