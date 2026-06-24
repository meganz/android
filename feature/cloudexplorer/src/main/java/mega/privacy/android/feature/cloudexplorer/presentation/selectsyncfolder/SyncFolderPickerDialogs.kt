package mega.privacy.android.feature.cloudexplorer.presentation.selectsyncfolder

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog asking the user to disable battery optimization so syncs can run in the background.
 */
@Composable
internal fun DisableBatteryOptimizationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        title = stringResource(sharedR.string.sync_dialog_battery_optimization_title),
        description = stringResource(sharedR.string.sync_battery_optimisation_banner),
        positiveButtonText = stringResource(sharedR.string.general_allow_button),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_do_not_allow_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

/**
 * Dialog to confirm the removal of the sync or backup connection of another device.
 */
@Composable
internal fun RemoveFolderConnectionDialog(
    deviceName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        title = stringResource(sharedR.string.sync_folder_connection_dialog_title),
        description = stringResource(
            sharedR.string.sync_folder_connection_dialog_message,
            deviceName,
        ),
        positiveButtonText = stringResource(sharedR.string.device_center_bottom_sheet_item_remove_connection),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun DisableBatteryOptimizationDialogPreview() {
    AndroidThemeForPreviews {
        DisableBatteryOptimizationDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun RemoveFolderConnectionDialogPreview() {
    AndroidThemeForPreviews {
        RemoveFolderConnectionDialog(
            deviceName = "My Device",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
