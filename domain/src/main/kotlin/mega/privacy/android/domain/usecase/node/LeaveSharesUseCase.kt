package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Delete nodes use case
 *
 * @property nodeRepository [NodeRepository]
 */
class LeaveSharesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository
) {

    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<NodeId>): MoveRequestResult.DeleteMovement {
        val results = supervisorScope {
            nodes.map { node ->
                async { runCatching { nodeRepository.leaveShareByHandle(node) } }
            }
        }.awaitAll()
        return MoveRequestResult.DeleteMovement(
            nodes.size,
            results.count { it.isFailure },
            nodes.map { it.longValue }
        )
    }
}
