package mega.privacy.android.shared.contact.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicContactDialog
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog shown when a scanned contact QR code resolves to a user that is not yet a contact.
 * Presents the contact's avatar, name and email with a confirm action and a cancel action.
 *
 * @param contactName Display name of the scanned contact.
 * @param contactEmail Email of the scanned contact.
 * @param avatar Avatar of the scanned contact; null renders a default-coloured initials avatar
 * derived from [contactName].
 * @param confirmActionText Label of the confirm button (e.g. "Invite"), so callers can repurpose
 * the dialog for other confirm actions.
 * @param onConfirm Invoked when the confirm button is clicked.
 * @param onDismiss Invoked when the dialog is cancelled or dismissed.
 */
@Composable
fun ScannedContactFoundDialog(
    contactName: String,
    contactEmail: String,
    avatar: AvatarData?,
    confirmActionText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicContactDialog(
        modifier = modifier.testTag(SCANNED_CONTACT_FOUND_DIALOG_TAG),
        contactName = contactName,
        contactEmail = contactEmail,
        contactAvatarFile = (avatar as? AvatarData.Image)?.file,
        contactAvatarColor = (avatar as? AvatarData.Initials)?.avatarColor ?: Color.Unspecified,
        positiveButtonText = confirmActionText,
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

/**
 * Informational dialog shown when the scanned contact QR code belongs to a user that is already
 * in the user's contact list.
 *
 * @param contactEmail Email of the already-added contact.
 * @param onDismiss Invoked when the dialog is acknowledged or dismissed.
 */
@Composable
fun ScannedContactAlreadyAddedDialog(
    contactEmail: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(SCANNED_CONTACT_ALREADY_ADDED_DIALOG_TAG),
        title = stringResource(sharedR.string.contacts_qr_invitation_not_sent_dialog_title),
        description = stringResource(
            sharedR.string.contacts_qr_already_contact_dialog_body,
            contactEmail,
        ),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

/**
 * Informational error dialog shown when the scanned QR code is not a valid MEGA contact link or
 * the contact link query failed.
 *
 * @param onDismiss Invoked when the dialog is acknowledged or dismissed.
 */
@Composable
fun ScannedContactInvalidCodeDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(SCANNED_CONTACT_INVALID_CODE_DIALOG_TAG),
        title = stringResource(sharedR.string.contacts_qr_invitation_not_sent_dialog_title),
        description = stringResource(sharedR.string.contacts_qr_invalid_code_dialog_body),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

/**
 * Informational dialog shown when the barcode scanner module is not installed yet. The module
 * download starts automatically, so the user is prompted to retry shortly.
 *
 * @param onDismiss Invoked when the dialog is acknowledged or dismissed.
 */
@Composable
fun ScannerModuleNotInstalledDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier.testTag(SCANNER_MODULE_NOT_INSTALLED_DIALOG_TAG),
        title = stringResource(sharedR.string.contacts_qr_scanner_not_ready_dialog_title),
        description = stringResource(sharedR.string.contacts_qr_scanner_not_ready_dialog_body),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

const val SCANNED_CONTACT_FOUND_DIALOG_TAG = "scanned_contact_dialogs:found_dialog"
const val SCANNED_CONTACT_ALREADY_ADDED_DIALOG_TAG = "scanned_contact_dialogs:already_added_dialog"
const val SCANNED_CONTACT_INVALID_CODE_DIALOG_TAG = "scanned_contact_dialogs:invalid_code_dialog"
const val SCANNER_MODULE_NOT_INSTALLED_DIALOG_TAG =
    "scanned_contact_dialogs:scanner_module_not_installed_dialog"

@CombinedThemePreviews
@Composable
private fun ScannedContactFoundDialogPreview() {
    AndroidThemeForPreviews {
        ScannedContactFoundDialog(
            contactName = "Alice Anderson",
            contactEmail = "alice@mega.co.nz",
            avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
            confirmActionText = "Invite",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ScannedContactAlreadyAddedDialogPreview() {
    AndroidThemeForPreviews {
        ScannedContactAlreadyAddedDialog(
            contactEmail = "alice@mega.co.nz",
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ScannedContactInvalidCodeDialogPreview() {
    AndroidThemeForPreviews {
        ScannedContactInvalidCodeDialog(onDismiss = {})
    }
}

@CombinedThemePreviews
@Composable
private fun ScannerModuleNotInstalledDialogPreview() {
    AndroidThemeForPreviews {
        ScannerModuleNotInstalledDialog(onDismiss = {})
    }
}
