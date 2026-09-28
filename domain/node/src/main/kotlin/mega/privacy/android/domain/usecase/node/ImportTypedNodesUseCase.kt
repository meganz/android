package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.ImportNodesResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
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
     * Quota and foreign-node failures are rethrown rather than counted, so the caller can warn
     * about them; every other failure is counted as an error, as the copy use cases do.
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
                }.onFailure {
                    if (it.shouldEmitErrorForNodeMovement()) throw it
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