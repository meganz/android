package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        val title = pluralStringResource(
            R.plurals.confirmation_leave_share_folder,
            totalNodes,
            totalNodes
        )
        MegaAlertDialog(
            text = title,
            confirmButtonText = stringResource(id = R.string.general_leave),
            cancelButtonText = stringResource(id = R.string.general_cancel),
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