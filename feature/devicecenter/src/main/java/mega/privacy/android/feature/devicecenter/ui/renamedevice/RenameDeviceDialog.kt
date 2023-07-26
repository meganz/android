package mega.privacy.android.feature.devicecenter.ui.renamedevice

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R

/**
 * Test tag for the Rename Device Dialog
 */
internal const val RENAME_DEVICE_DIALOG_TAG = "rename_device_dialog:input_dialog"

/**
 * A Composable Dialog that allows the User to rename a Device
 *
 * @param renameDeviceViewModel The [RenameDeviceViewModel] that handles business logic
 * @param deviceId The Device ID identifying the Device to be renamed
 * @param oldDeviceName The old Device Name
 * @param onRenameSuccessful Lambda that executes code when a successful rename occurs
 * @param onRenameCancelled Lambda that executes code when the User cancels the renaming procedure
 */
@Composable
internal fun RenameDeviceDialog(
    renameDeviceViewModel: RenameDeviceViewModel = viewModel(),
    deviceId: String,
    oldDeviceName: String,
    onRenameSuccessful: () -> Unit,
    onRenameCancelled: () -> Unit,
) {
    val uiState by renameDeviceViewModel.state.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.renameSuccessfulEvent,
        onConsumed = renameDeviceViewModel::onResetRenameSuccessfulEvent,
        action = onRenameSuccessful,
    )
    InputDialog(
        modifier = Modifier.testTag(RENAME_DEVICE_DIALOG_TAG),
        title = stringResource(id = R.string.device_center_rename_device_dialog_title),
        confirmButtonText = stringResource(id = R.string.device_center_rename_device_dialog_positive_button),
        cancelButtonText = stringResource(id = R.string.device_center_rename_device_dialog_negative_button),
        text = oldDeviceName,
        onConfirm = { newDeviceName ->
            renameDeviceViewModel.renameDevice(
                deviceId = deviceId,
                deviceName = newDeviceName,
            )
        },
        onDismiss = onRenameCancelled,
    )
}

@CombinedThemePreviews
@Composable
private fun RenameDeviceDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        InputDialog(
            title = stringResource(id = R.string.device_center_rename_device_dialog_title),
            confirmButtonText = stringResource(id = R.string.device_center_rename_device_dialog_positive_button),
            cancelButtonText = stringResource(id = R.string.device_center_rename_device_dialog_negative_button),
            text = "Samsung Galaxy S21 FE",
            onConfirm = {},
            onDismiss = {},
        )
    }
}