package mega.privacy.android.core.nodecomponents.dialog.leaveshare

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Leave share Dialog
 * @param leaveShareDialogViewModel [mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogViewModel]
 * @param handles list of handles
 * @param onDismiss
 */
@Composable
fun LeaveShareDialogM3(
    handles: List<Long>,
    onDismiss: () -> Unit,
    leaveShareDialogViewModel: LeaveShareDialogViewModel = hiltViewModel(),
) {
    LeaveShareDialogBody(
        onConfirm = {
            leaveShareDialogViewModel.onLeaveShareConfirmClicked(handles)
            onDismiss()
        },
        onCancel = onDismiss,
        totalNodes = handles.size
    )
}

@Composable
private fun LeaveShareDialogBody(
    totalNodes: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BasicDialog(
        description = stringResource(sharedR.string.leave_shared_folder_confirmation_message),
        positiveButtonText = stringResource(id = sharedR.string.general_leave),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = onConfirm,
        onNegativeButtonClicked = onCancel,
        onDismiss = onCancel
    )
}

@CombinedThemePreviews
@Composable
private fun LeaveShareDialogBodyPreview() {
    AndroidThemeForPreviews {
        LeaveShareDialogBody(
            totalNodes = 2,
            onConfirm = {},
            onCancel = {}
        )
    }
}