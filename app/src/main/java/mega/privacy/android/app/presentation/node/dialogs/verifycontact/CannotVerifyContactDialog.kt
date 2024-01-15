package mega.privacy.android.app.presentation.node.dialogs.verifycontact

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Dialog to show when the user tries to verify a contact that is not in the contact list
 *
 * @param email Email of the contact to verify
 * @param onDismiss dismiss callback of dialog
 */
@Composable
fun CannotVerifyContactDialog(email: String?, onDismiss: () -> Unit) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ConfirmationDialog(
            title = stringResource(id = R.string.shared_items_contact_not_in_contact_list_dialog_title),
            text = stringResource(id = R.string.shared_items_contact_not_in_contact_list_dialog_content)
                .format(email),
            confirmButtonText = stringResource(id = R.string.general_ok),
            cancelButtonText = null,
            onConfirm = onDismiss,
            onDismiss = onDismiss,
        )
    }
}