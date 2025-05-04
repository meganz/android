package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.filecontact.model.ShareRecipientPreviewParameterProvider
import mega.privacy.android.domain.entity.shares.ShareRecipient

@Composable
internal fun VerifyRemovalDialog(
    selectedItems: List<ShareRecipient>,
    onDismiss: () -> Unit,
    removeContacts: (List<ShareRecipient>) -> Unit,
) {
    if (selectedItems.isEmpty()) {
        return
    }

    val title = if (selectedItems.size > 1) {
        pluralStringResource(
            R.plurals.remove_multiple_contacts_shared_folder,
            selectedItems.size,
            selectedItems.size
        )
    } else {
        stringResource(R.string.remove_contact_shared_folder, selectedItems[0].email)
    }


    BasicDialog(
        title = title,
        positiveButtonText = stringResource(R.string.general_remove),
        onPositiveButtonClicked = {
            onDismiss()
            removeContacts(selectedItems)
        },
        negativeButtonText = stringResource(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun VerifyRemovalDialogPreview(@PreviewParameter(ShareRecipientPreviewParameterProvider::class) recipients: List<ShareRecipient>) {
    AndroidThemeForPreviews {
        VerifyRemovalDialog(
            selectedItems = recipients,
            onDismiss = {},
            removeContacts = { _ -> },
        )
    }
}