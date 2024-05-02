package mega.privacy.android.domain.usecase.node.namecollision

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.namecollision.TypedNodeNameCollisionResult
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import javax.inject.Inject

/**
 * Check general nodes name collision use case (Cloud driver node)
 */
class CheckTypedNodeNameCollisionUseCase @Inject constructor(
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val getChildNodeUseCase: GetChildNodeUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
) {
    /**
     * Invoke
     *
     * @param nodes List of [TypedNode]
     * @param handleWhereToImport Handle where to import
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
        handleWhereToImport: Long,
    ): TypedNodeNameCollisionResult {
        val noConflictNodes = mutableListOf<TypedNode>()
        val conflictNodes = mutableListOf<NodeNameCollision>()
        getNodeByHandleUseCase(handleWhereToImport)
            ?.takeUnless { isNodeInRubbishBinUseCase(NodeId(handleWhereToImport)) }
            ?.let { parentNode ->
                nodes.forEach { node ->
                    getChildNodeUseCase(
                        NodeId(handleWhereToImport),
                        node.name
                    )?.let { conflictNode ->
                        conflictNodes.add(createNodeNameCollision(node, parentNode, conflictNode))
                    } ?: run {
                        noConflictNodes.add(node)
                    }
                }
            } ?: run { noConflictNodes.addAll(nodes) }

        return TypedNodeNameCollisionResult(noConflictNodes, conflictNodes)
    }

    private fun createNodeNameCollision(
        currentNode: TypedNode,
        parent: UnTypedNode,
        conflictNode: UnTypedNode,
    ) = NodeNameCollision.Default(
        collisionHandle = conflictNode.id.longValue,
        nodeHandle = currentNode.id.longValue,
        parentHandle = parent.id.longValue,
        name = currentNode.name,
        size = (currentNode as? FileNode)?.size ?: 0,
        childFolderCount = (parent as? FolderNode)?.childFolderCount ?: 0,
        childFileCount = (parent as? FolderNode)?.childFileCount ?: 0,
        lastModified = if (currentNode is FileNode) currentNode.modificationTime else currentNode.creationTime,
        isFile = currentNode is FileNode
    )
}
