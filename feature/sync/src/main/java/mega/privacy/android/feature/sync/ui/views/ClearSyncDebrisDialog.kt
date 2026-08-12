package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R

@Composable
internal fun ClearSyncDebrisDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        modifier = modifier,
        title = stringResource(R.string.settings_sync_clear_debris_dialog_title),
        description = stringResource(R.string.settings_sync_clear_debris_dialog_body),
        positiveButtonText = stringResource(R.string.settings_sync_clear_debris_dialog_continue),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(R.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun ClearSyncDebrisDialogPreview() {
    AndroidThemeForPreviews {
        ClearSyncDebrisDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}
