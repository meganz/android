package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R

@Composable
internal fun BackupNodeShareWarningDialog(
    onPositiveButtonClicked: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        title = stringResource(R.string.backup_share_permission_title),
        description = stringResource(R.string.backup_share_permission_text),
        positiveButtonText = stringResource(R.string.button_permission_info),
        onPositiveButtonClicked = onPositiveButtonClicked ?: onDismiss,
        negativeButtonText = stringResource(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button).takeIf { onPositiveButtonClicked != null },
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun VerifyRemovalDialogOptionsPreview() {
    AndroidThemeForPreviews {
        BackupNodeShareWarningDialog(
            onPositiveButtonClicked = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VerifyRemovalDialogInfoPreview() {
    AndroidThemeForPreviews {
        BackupNodeShareWarningDialog(
            onDismiss = {},
        )
    }
}