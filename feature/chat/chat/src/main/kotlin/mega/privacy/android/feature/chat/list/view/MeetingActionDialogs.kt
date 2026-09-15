package mega.privacy.android.feature.chat.list.view

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Dialog telling the user a meeting cannot be started or joined because they are already in a call.
 *
 * @param message Message explaining which action was refused.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
internal fun MeetingBlockedByCallDialog(
    @StringRes message: Int,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(MEETING_BLOCKED_BY_CALL_DIALOG_TAG),
        title = stringResource(sharedR.string.chat_meeting_blocked_by_call_dialog_title),
        description = stringResource(message),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

/**
 * Dialog telling the user the Bluetooth permission is required before a meeting can be started.
 *
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
internal fun BluetoothPermissionRequiredDialog(
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(BLUETOOTH_PERMISSION_DIALOG_TAG),
        title = stringResource(sharedR.string.chat_start_meeting_bluetooth_permission_title),
        description = stringResource(sharedR.string.chat_start_meeting_bluetooth_permission_warning),
        positiveButtonText = stringResource(sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

internal const val MEETING_BLOCKED_BY_CALL_DIALOG_TAG =
    "meeting_action_dialogs:blocked_by_call"
internal const val BLUETOOTH_PERMISSION_DIALOG_TAG =
    "meeting_action_dialogs:bluetooth_permission"

@CombinedThemePreviews
@Composable
private fun MeetingBlockedByCallStartPreview() {
    AndroidThemeForPreviews {
        MeetingBlockedByCallDialog(
            message = sharedR.string.chat_start_meeting_already_in_call_error,
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeetingBlockedByCallJoinPreview() {
    AndroidThemeForPreviews {
        MeetingBlockedByCallDialog(
            message = sharedR.string.can_only_join_one_call_error_message,
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BluetoothPermissionRequiredPreview() {
    AndroidThemeForPreviews {
        BluetoothPermissionRequiredDialog(onDismiss = {})
    }
}
