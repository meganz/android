package mega.privacy.android.app.presentation.validator.toolbaractions.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission

@Immutable
data class ToolbarActionsRequest(
    val modifierItem: ToolbarActionsModifierItem,
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
    val accessPermission: AccessPermission,
    val canBeMovedToRubbishBin: Boolean,
)

@Immutable
sealed interface SelectedNodeType {

    /**
     * Representation for [FolderNode].
     */
    data class Folder(
        val isShared: Boolean,
        val isPendingShare: Boolean,
    ) : SelectedNodeType

    /**
     * Representation for [FileNode].
     */
    data object File : SelectedNodeType

    /**
     * Representation for [TypedNode].
     */
    data object Typed : SelectedNodeType

    companion object {
        fun toSelectedNodeType(from: TypedNode): SelectedNodeType = when (from) {
            is FileNode -> File

            is FolderNode -> Folder(
                isShared = from.isShared,
                isPendingShare = from.isPendingShare
            )

            else -> Typed
        }
    }
}
