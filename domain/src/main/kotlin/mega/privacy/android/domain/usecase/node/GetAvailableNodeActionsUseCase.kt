package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * Use case to get the available [NodeAction]s for the given node
 */
class GetAvailableNodeActionsUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
) {
    /**
     * Invocation function
     *
     * @param typedNode A [TypedNode] used to retrieve all available Node Actions
     * @return a list of available [NodeAction]s for the specified [TypedNode]
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
                typedNode.parentId.longValue == -1L || getNodeByIdUseCase(typedNode.parentId)?.parentId?.longValue == -1L
            if (!typedNode.isTakenDown) {
                add(NodeAction.Download)
                add(NodeAction.Copy)
                if (incomingShareFirstLevel) {
                    add(NodeAction.Leave)
                }
            }
            val accessPermission = getNodeAccessPermission(typedNode.id)
            if (!isNodeInBackupsUseCase(typedNode.id.longValue) && (accessPermission == AccessPermission.OWNER || accessPermission == AccessPermission.FULL)) {
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
            if (!isNodeInBackupsUseCase(typedNode.id.longValue)) {
                add(NodeAction.MoveToRubbishBin)
                add(NodeAction.Rename)
                add(NodeAction.Move)
            }
        }
    }

}