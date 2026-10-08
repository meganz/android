package mega.privacy.android.feature.cloudexplorer.presentation.move

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.destination.MoveNavKey

/**
 * The folder the Move picker must not accept as the target while [folderId] is open: [folderId]
 * itself when it is one of the moved nodes or inside one of them, otherwise the moved nodes'
 * current parent ([MoveNavKey.disabledTargetId]).
 *
 * @param folderId the folder currently open in the Move picker
 * @param moveNavKey the Move flow being picked for
 */
@Composable
internal fun rememberMoveDisabledTargetId(folderId: NodeId, moveNavKey: MoveNavKey): NodeId {
    val viewModel =
        hiltViewModel<MoveTargetViewModel, MoveTargetViewModel.Factory> { factory ->
            factory.create(
                MoveTargetViewModel.Args(
                    targetId = folderId,
                    movedNodeIds = moveNavKey.nodeIds,
                )
            )
        }
    val isInsideMovedNodes by viewModel.isInsideMovedNodes.collectAsStateWithLifecycle()
    return if (isInsideMovedNodes) folderId else moveNavKey.disabledTargetId
}
