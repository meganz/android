package mega.privacy.android.feature.devicecenter.ui.renamedevice

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.renamedevice.model.RenameDeviceState
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog

/**
 * Test tag for the Rename Device Dialog
 */
internal const val RENAME_DEVICE_DIALOG_TAG = "rename_device_dialog:input_dialog"

/**
 * A Composable Dialog that allows the User to rename a Device
 *
 * @param deviceId The Device ID identifying the Device to be renamed
 * @param oldDeviceName The old Device Name
 * @param existingDeviceNames The list of existing Device Names
 * @param onRenameSuccessful Lambda that is triggered when a successful rename occurs
 * @param onRenameCancelled Lambda that is triggered when the User cancels the renaming procedure
 * @param renameDeviceViewModel The [RenameDeviceViewModel] that handles business logic
 */
@Composable
internal fun RenameDeviceDialog(
    deviceId: String,
    oldDeviceName: String,
    existingDeviceNames: List<String>,
    onRenameSuccessful: () -> Unit,
    onRenameCancelled: () -> Unit,
    renameDeviceViewModel: RenameDeviceViewModel = hiltViewModel(),
) {
    val uiState by renameDeviceViewModel.state.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.renameSuccessfulEvent,
        onConsumed = renameDeviceViewModel::resetRenameSuccessfulEvent,
        action = onRenameSuccessful,
    )
    RenameDeviceDialogBody(
        uiState = uiState,
        oldDeviceName = oldDeviceName,
        onRenameConfirmed = { newDeviceName ->
            renameDeviceViewModel.renameDevice(
                deviceId = deviceId,
                newDeviceName = newDeviceName,
                existingDeviceNames = existingDeviceNames,
            )
        },
        onRenameCancelled = {
            renameDeviceViewModel.clearErrorMessage()
            onRenameCancelled.invoke()
        },
    )
}

/**
 * A Composable that serves as the Body for the Rename Device Dialog
 *
 * @param uiState The [RenameDeviceState]
 * @param oldDeviceName The old Device Name
 * @param onRenameConfirmed Lambda that is triggered when the "Rename" Button is clicked
 * @param onRenameCancelled Lambda that is triggered when the Dialog is dismissed
 */
@Composable
private fun RenameDeviceDialogBody(
    uiState: RenameDeviceState,
    oldDeviceName: String,
    onRenameConfirmed: (String) -> Unit,
    onRenameCancelled: () -> Unit,
) {
    // Saves the input across configuration changes
    var initialInput by rememberSaveable { mutableStateOf(oldDeviceName) }

    InputDialog(
        modifier = Modifier.testTag(RENAME_DEVICE_DIALOG_TAG),
        title = stringResource(id = R.string.device_center_rename_device_dialog_title),
        confirmButtonText = stringResource(id = R.string.device_center_rename_device_dialog_positive_button),
        cancelButtonText = stringResource(id = R.string.device_center_rename_device_dialog_negative_button),
        text = initialInput,
        onInputChange = { initialInput = it },
        error = uiState.errorMessage?.let { nonNullErrorMessage ->
            if (nonNullErrorMessage == R.string.device_center_rename_device_dialog_error_message_invalid_characters) {
                stringResource(nonNullErrorMessage).replace(
                    oldValue = "%1\$s",
                    newValue = "\" * / : < > ? \\ |"
                )
            } else {
                stringResource(nonNullErrorMessage)
            }
        },
        onConfirm = onRenameConfirmed,
        onDismiss = onRenameCancelled,
    )
}

/**
 * A Preview Composable for the [RenameDeviceDialogBody]
 */
@CombinedThemePreviews
@Composable
private fun PreviewRenameDeviceDialogBody(
    @PreviewParameter(RenameDeviceDialogBodyPreviewProvider::class) renameDeviceState: RenameDeviceState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RenameDeviceDialogBody(
            uiState = renameDeviceState,
            oldDeviceName = "Samsung Galaxy S21 FE",
            onRenameConfirmed = {},
            onRenameCancelled = {},
        )
    }
}

/**
 * A class that provides Preview Parameters for the [RenameDeviceDialogBody]
 */
private class RenameDeviceDialogBodyPreviewProvider : PreviewParameterProvider<RenameDeviceState> {
    override val values: Sequence<RenameDeviceState>
        get() = sequenceOf(
            RenameDeviceState(),
            RenameDeviceState(errorMessage = R.string.device_center_rename_device_dialog_error_message_empty_device_name),
            RenameDeviceState(errorMessage = R.string.device_center_rename_device_dialog_error_message_maximum_character_length_exceeded),
            RenameDeviceState(errorMessage = R.string.device_center_rename_device_dialog_error_message_name_already_exists),
            RenameDeviceState(errorMessage = R.string.device_center_rename_device_dialog_error_message_invalid_characters),
        )
}