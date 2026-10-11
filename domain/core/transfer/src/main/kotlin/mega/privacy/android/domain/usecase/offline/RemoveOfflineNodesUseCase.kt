package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Remove list of offline nodes
 *
 */
class RemoveOfflineNodesUseCase @Inject constructor(
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
) {
    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<NodeId>): MoveRequestResult.RemoveOffline {
        val semaphore = Semaphore(8)
        val results = supervisorScope {
            nodes.map { node ->
                async {
                    semaphore.withPermit {
                        runCatching { removeOfflineNodeUseCase(node) }
                    }
                }
            }
        }.awaitAll()
        return MoveRequestResult.RemoveOffline(
            nodes.size,
            results.count { it.isFailure },
        )
    }
}