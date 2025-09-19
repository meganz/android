package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Leave share Dialog
 * @param leaveShareDialogViewModel [mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogViewModel]
 * @param handles list of handles
 * @param onDismiss
 */
@Composable
fun LeaveShareDialog(
    handles: List<Long>,
    leaveShareDialogViewModel: LeaveShareDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    LeaveShareDialogBody(
        onOkClicked = {
            leaveShareDialogViewModel.onLeaveShareConfirmClicked(handles)
            onDismiss()
        },
        onCancelClicked = onDismiss,
        totalNodes = handles.size
    )
}

@Composable
private fun LeaveShareDialogBody(
    totalNodes: Int,
    onOkClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val title = stringResource(sharedR.string.leave_shared_folder_confirmation_message)
        MegaAlertDialog(
            text = title,
            confirmButtonText = stringResource(id = sharedR.string.general_leave),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onOkClicked,
            onDismiss = onCancelClicked
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LeaveShareDialogBodyPreview() {
    LeaveShareDialogBody(
        totalNodes = 2,
        onOkClicked = {},
        onCancelClicked = {}
    )
}