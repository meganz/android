package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeById
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * Use case to get the available [NodeAction]s for the given node
 */
class GetAvailableNodeActionsUseCase @Inject constructor(
    private val getNodeById: GetNodeById,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getNodeAccessPermission: GetNodeAccessPermission,
) {
    /**
     * @return a list of available [NodeAction]s for the given node
     */
    suspend operator fun invoke(
        typedNode: TypedNode,
    ) = if (isNodeInRubbish(typedNode.id.longValue)) {
        listOf(NodeAction.Delete)
    } else mutableListOf<NodeAction>().apply {
        if (!typedNode.isTakenDown && typedNode is FileNode) {
            add(NodeAction.SendToChat)
        }
        if (typedNode.isIncomingShare) {
            val incomingShareFirstLevel =
                typedNode.parentId.longValue == -1L || getNodeById(typedNode.parentId).parentId.longValue == -1L
            if (!typedNode.isTakenDown) {
                add(NodeAction.Download)
                add(NodeAction.Copy)
                if (incomingShareFirstLevel) {
                    add(NodeAction.Leave)
                }
            }
            val accessPermission = getNodeAccessPermission(typedNode.id)
            if (accessPermission == AccessPermission.OWNER || accessPermission == AccessPermission.FULL) {
                add(NodeAction.Rename)
                if (incomingShareFirstLevel) {
                    add(NodeAction.MoveToRubbishBin)
                }
            }
        } else {
            if (!typedNode.isTakenDown) {
                add(NodeAction.Download)
                add(NodeAction.Copy)
                if (typedNode !is FileNode) {
                    add(NodeAction.ShareFolder)
                }
                if (typedNode.exportedData != null) {
                    add(NodeAction.ManageLink)
                    add(NodeAction.RemoveLink)
                } else {
                    add(NodeAction.GetLink)
                }
            } else {
                add(NodeAction.DisputeTakedown)
            }
            add(NodeAction.MoveToRubbishBin)
            add(NodeAction.Rename)
            add(NodeAction.Move)
        }
    }

}