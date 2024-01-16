package mega.privacy.android.app.presentation.node.dialogs.sharefolder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Share folder dialog
 * @param nodeIds List of [NodeId]
 * @param modifier [Modifier]
 * @param shareFolderDialogViewModel [ShareFolderDialogViewModel]
 * @param onDismiss
 */
@Composable
fun ShareFolderDialog(
    nodeIds: List<NodeId>,
    modifier: Modifier = Modifier,
    shareFolderDialogViewModel: ShareFolderDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onOkClicked: () -> Unit,
) {
    LaunchedEffect(Unit) {
        shareFolderDialogViewModel.getDialogContents(nodeIds)
    }
    val shareFolderState by shareFolderDialogViewModel.state.collectAsStateWithLifecycle()

    ShareFolderDialogBody(
        modifier = modifier,
        state = shareFolderState,
        onDismiss = onDismiss,
        onOkClicked = onOkClicked
    )
}

@Composable
private fun ShareFolderDialogBody(
    modifier: Modifier,
    state: ShareFolderDialogState,
    onDismiss: () -> Unit,
    onOkClicked: () -> Unit,
) {

    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MegaAlertDialog(
            title = stringResource(id = state.title),
            body = stringResource(id = state.info),
            icon = R.drawable.il_serious_warning,
            modifier = modifier,
            confirmButtonText = stringResource(id = state.positiveButton),
            cancelButtonText = state.negativeButton?.let { stringResource(id = it) },
            onConfirm = {
                onDismiss()
                if (state.shouldHandlePositiveClick) {
                    onOkClicked()
                }
            },
            onDismiss = onDismiss,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareFolderDialogBodyPreview() {
    ShareFolderDialogBody(
        modifier = Modifier,
        state = ShareFolderDialogState(
            title = R.string.backup_share_permission_title,
            info = R.string.backup_multi_share_permission_text,
            positiveButton = R.string.button_permission_info,
            negativeButton = null,
            shouldHandlePositiveClick = false
        ),
        onOkClicked = {},
        onDismiss = {}
    )
}