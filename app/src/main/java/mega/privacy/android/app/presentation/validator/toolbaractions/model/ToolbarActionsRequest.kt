package mega.privacy.android.app.presentation.validator.toolbaractions.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission

@Immutable
data class ToolbarActionsRequest(
    val selectedNodes: List<SelectedNode>,
    val totalNodes: Int,
)

@Immutable
data class SelectedNode(
    val id: Long,
    val type: SelectedNodeType,
    val isTakenDown: Boolean,
    val isExported: Boolean,
    val isIncomingShare: Boolean,
    val accessPermissions: List<AccessPermission>,
    val canBeMovedToRubbishBin: Boolean,
)

@Immutable
sealed interface SelectedNodeType {

    data class Folder(
        val isShared: Boolean,
        val isPendingShare: Boolean,
    ) : SelectedNodeType

    data object File : SelectedNodeType

    companion object {
        fun toSelectedNodeType(from: TypedNode): SelectedNodeType = when (from) {
            is FolderNode -> Folder(
                isShared = from.isShared,
                isPendingShare = from.isPendingShare
            )

            else -> File
        }
    }
}
