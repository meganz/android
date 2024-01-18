package mega.privacy.android.app.presentation.node.dialogs.sharefolder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    shareFolderDialogViewModel: ShareFolderDialogViewModel,
    onDismiss: () -> Unit,
    onOkClicked: () -> Unit,
) {
    val shareFolderState by shareFolderDialogViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        shareFolderDialogViewModel.getDialogContents(nodeIds)
    }

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
        state.info?.let { infoRes ->
            MegaAlertDialog(
                title = stringResource(id = R.string.backup_share_permission_title),
                body = stringResource(id = infoRes),
                icon = R.drawable.il_serious_warning,
                modifier = modifier,
                confirmButtonText = stringResource(id = state.positiveButton),
                cancelButtonText = state.negativeButton?.let { stringResource(id = it) },
                onConfirm = {
                    onDismiss()
                    onOkClicked()
                },
                onDismiss = onDismiss,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ShareFolderDialogBodyPreview() {
    ShareFolderDialogBody(
        modifier = Modifier,
        state = ShareFolderDialogState(
            info = R.string.backup_multi_share_permission_text,
            positiveButton = R.string.button_permission_info,
            negativeButton = null,
        ),
        onOkClicked = {},
        onDismiss = {}
    )
}