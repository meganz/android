package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import javax.inject.Inject

/**
 *  Use Case to copy list of typed nodes
 */
class CopyTypedNodesUseCase @Inject constructor(
    private val copyTypedNodeUseCase: CopyTypedNodeUseCase,
) {
    /**
     * Invoke
     *
     * @param nodesToCopy The list of TypedNode to copy
     * @param newNodeParent the Node when the chat node will be moved to
     * @return MoveRequestResult
     */
    suspend operator fun invoke(
        nodesToCopy: List<TypedNode>,
        newNodeParent: NodeId,
    ): MoveRequestResult {
        val results = coroutineScope {
            val semaphore = Semaphore(10)
            nodesToCopy.map { typedNode ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            copyTypedNodeUseCase(
                                nodeToCopy = typedNode,
                                newNodeName = null,
                                newNodeParent = newNodeParent
                            )
                        }.recover {
                            if (it.shouldEmitErrorForNodeMovement()) throw it
                            return@async Result.failure(it)
                        }
                    }
                }
            }
        }.awaitAll()
        val successCount = results.count { it.isSuccess }
        return MoveRequestResult.Copy(
            count = results.size,
            errorCount = results.size - successCount,
        )
    }
}