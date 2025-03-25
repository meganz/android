package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Leave share Dialog
 * @param leaveShareDialogViewModel [LeaveShareDialogViewModel]
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
        val title = pluralStringResource(
            R.plurals.confirmation_leave_share_folder,
            totalNodes,
            totalNodes
        )
        MegaAlertDialog(
            text = title,
            confirmButtonText = stringResource(id = R.string.general_leave),
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