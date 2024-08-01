package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R

/**
 * Clear sync debris dialog shown when user wants to clear the debris left by sync
 *
 * @param onDismiss - Callback when dialog is dismissed by clicks outside the dialog or cancel button
 * @param onConfirm - Callback when dialog is confirmed
 */
@Composable
internal fun ClearSyncDebrisDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialog(
        title = stringResource(R.string.settings_sync_clear_debris_dialog_title),
        text = stringResource(R.string.settings_sync_clear_debris_dialog_body),
        confirmButtonText = stringResource(R.string.settings_sync_clear_debris_dialog_continue),
        cancelButtonText = stringResource(R.string.settings_sync_clear_debris_dialog_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
@Preview
@CombinedThemePreviews
private fun ClearSyncDebrisDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ClearSyncDebrisDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}