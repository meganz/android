package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.ImportNodesResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.namecollision.CheckTypedNodeNameCollisionUseCase
import javax.inject.Inject

/**
 * Use case to import nodes from messages
 *
 * @property checkTypedNodeNameCollisionUseCase
 * @property copyTypedNodeUseCase
 */
class ImportTypedNodesUseCase @Inject constructor(
    private val checkTypedNodeNameCollisionUseCase: CheckTypedNodeNameCollisionUseCase,
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
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
    ): ImportNodesResult {
        checkTypedNodeNameCollisionUseCase(nodes, handleWhereToImport).let { result ->
            val parentNodeId = NodeId(handleWhereToImport)
            val copyResult = result.noConflictNodes.map { node ->
                runCatching {
                    copyTypedNodeUseCase(node, parentNodeId)
                }
            }
            return ImportNodesResult(
                copyResult.count { it.isSuccess },
                copyResult.count { it.isFailure },
                result.conflictNodes
            )
        }
    }
}