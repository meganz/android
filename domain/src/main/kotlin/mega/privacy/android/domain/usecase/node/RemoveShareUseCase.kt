package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.RemoveShareResult
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Remove share use case
 *
 * @property nodeRepository Node repository
 */
class RemoveShareUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodeIds List of node ids
     */
    suspend operator fun invoke(nodeIds: List<NodeId>): RemoveShareResult {
        val allShares = nodeIds.map { nodeRepository.getNodeOutgoingShares(it) }.flatten()
        val result = coroutineScope {
            allShares.map {
                async {
                    runCatching {
                        nodeRepository.setShareAccess(
                            nodeId = NodeId(longValue = it.nodeHandle),
                            accessPermission = AccessPermission.UNKNOWN,
                            email = it.user.orEmpty()
                        )
                    }
                }
            }.awaitAll()
        }
        return RemoveShareResult(
            successCount = result.count { it.isSuccess },
            errorCount = result.count { it.isFailure }
        )
    }
}