package mega.privacy.android.app.presentation.node.dialogs.sharefolder.warning

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogState
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogType
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogViewModel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

internal data class ShareFolderUiAttr(
    @StringRes val info: Int,
    @StringRes val positiveButton: Int,
    @StringRes val negativeButton: Int? = null,
)

/**
 * Share folder dialog
 * @param nodeIds List of [NodeId]
 * @param shareFolderDialogViewModel [ShareFolderDialogViewModel]
 * @param onDismiss
 * @param onOkClicked
 */
@Composable
fun ShareFolderDialog(
    nodeIds: List<NodeId>,
    onDismiss: () -> Unit,
    onOkClicked: (List<TypedNode>) -> Unit,
    shareFolderDialogViewModel: ShareFolderDialogViewModel = hiltViewModel(),
) {
    val shareFolderState by shareFolderDialogViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        shareFolderDialogViewModel.getDialogContents(nodeIds)
    }

    ShareFolderDialogBody(
        state = shareFolderState,
        onDismiss = onDismiss,
        onOkClicked = onOkClicked
    )
}

@Composable
private fun ShareFolderDialogBody(
    state: ShareFolderDialogState,
    onDismiss: () -> Unit,
    onOkClicked: (List<TypedNode>) -> Unit,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        state.dialogType?.let { type ->
            val attr = remember(type) {
                when (type) {
                    is ShareFolderDialogType.Multiple -> ShareFolderUiAttr(
                        info = R.string.backup_multi_share_permission_text,
                        positiveButton = R.string.general_positive_button,
                        negativeButton = sharedR.string.general_dialog_cancel_button
                    )

                    is ShareFolderDialogType.Single -> ShareFolderUiAttr(
                        info = R.string.backup_share_permission_text,
                        positiveButton = R.string.button_permission_info,
                        negativeButton = null
                    )
                }
            }

            MegaAlertDialog(
                title = stringResource(id = R.string.backup_share_permission_title),
                body = stringResource(id = attr.info),
                icon = R.drawable.il_serious_warning,
                confirmButtonText = stringResource(id = attr.positiveButton),
                cancelButtonText = attr.negativeButton?.let { stringResource(id = it) },
                onConfirm = {
                    onDismiss()
                    onOkClicked(type.nodes)
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
private fun ShareFolderDialogBodyPreviewMulti() {
    ShareFolderDialogBody(
        state = ShareFolderDialogState(
            dialogType = ShareFolderDialogType.Multiple(
                nodeList = emptyList()
            ),
        ),
        onOkClicked = {},
        onDismiss = {}
    )
}

@CombinedThemePreviews
@Composable
private fun ShareFolderDialogBodyPreviewSingle() {
    ShareFolderDialogBody(
        state = ShareFolderDialogState(
            dialogType = ShareFolderDialogType.Single(
                nodeList = emptyList()
            ),
        ),
        onOkClicked = {},
        onDismiss = {}
    )
}