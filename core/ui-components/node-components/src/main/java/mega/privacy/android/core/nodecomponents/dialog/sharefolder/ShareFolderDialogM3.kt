package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
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
 * @param onConfirm
 */
@Composable
fun ShareFolderDialogM3(
    nodeIds: List<NodeId>,
    onDismiss: () -> Unit,
    onConfirm: (List<TypedNode>) -> Unit,
    shareFolderDialogViewModel: ShareFolderDialogViewModel = hiltViewModel(),
) {
    val shareFolderState by shareFolderDialogViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        shareFolderDialogViewModel.getDialogContents(nodeIds)
    }

    ShareFolderDialogM3View(
        state = shareFolderState,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ShareFolderDialogM3View(
    state: ShareFolderDialogState,
    onDismiss: () -> Unit,
    onConfirm: (List<TypedNode>) -> Unit,
) {
    state.dialogType?.let { type ->
        val attr = remember(type) {
            when (type) {
                is ShareFolderDialogType.Multiple -> ShareFolderUiAttr(
                    info = sharedR.string.backup_multi_share_permission_text,
                    positiveButton = sharedR.string.button_continue,
                    negativeButton = sharedR.string.general_dialog_cancel_button
                )

                is ShareFolderDialogType.Single -> ShareFolderUiAttr(
                    info = sharedR.string.backup_share_permission_text,
                    positiveButton = sharedR.string.button_permission_info,
                    negativeButton = null
                )
            }
        }

        BasicDialog(
            title = stringResource(id = sharedR.string.backup_share_permission_title),
            description = stringResource(id = attr.info),
            positiveButtonText = stringResource(id = attr.positiveButton),
            negativeButtonText = attr.negativeButton?.let { stringResource(id = it) },
            onPositiveButtonClicked = {
                onConfirm(type.nodes)
                onDismiss()
            },
            onNegativeButtonClicked = onDismiss,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareFolderDialogBodyPreviewMulti() {
    AndroidThemeForPreviews {
        ShareFolderDialogM3View(
            state = ShareFolderDialogState(
                dialogType = ShareFolderDialogType.Multiple(
                    nodeList = emptyList()
                ),
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ShareFolderDialogBodyPreviewSingle() {
    AndroidThemeForPreviews {
        ShareFolderDialogM3View(
            state = ShareFolderDialogState(
                dialogType = ShareFolderDialogType.Single(
                    nodeList = emptyList()
                ),
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}