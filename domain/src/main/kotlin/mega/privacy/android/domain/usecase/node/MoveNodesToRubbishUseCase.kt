package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.ensureActive
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Move nodes to rubbish use case
 *
 */
class MoveNodesToRubbishUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @return
     */
    suspend operator fun invoke(nodes: List<Long>): MoveRequestResult {
        val rubbishNode = nodeRepository.getRubbishNode()
            ?: throw NodeDoesNotExistsException()
        val oldParentHandle = if (nodes.size == 1) {
            nodeRepository.getNodeByHandle(nodes.first())?.parentId?.longValue
        } else {
            nodeRepository.getInvalidHandle()
        }
        val results = nodes.map { handle ->
            runCatching {
                moveNodeUseCase(NodeId(handle), rubbishNode.id)
            }.onSuccess {
                nodeRepository.stopSharingNode(NodeId(handle))
            }.also {
                coroutineContext.ensureActive()
            }
        }
        return MoveRequestResult.RubbishMovement(
            count = nodes.size,
            errorCount = results.count { it.isFailure },
            oldParentHandle = oldParentHandle,
        )
    }
}