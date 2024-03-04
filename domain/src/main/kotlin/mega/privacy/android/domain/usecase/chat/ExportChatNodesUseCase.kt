package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Export chat nodes use case
 *
 */
class ExportChatNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     * @param nodes List of [TypedNode] to export
     */
    suspend operator fun invoke(
        nodes: List<TypedNode>,
    ): Map<NodeId, String> = supervisorScope {
        nodes.map { node ->
            async {
                runCatching {
                    node.id to nodeRepository.exportNode(node)
                }
            }
        }
    }.awaitAll()
        .mapNotNull { it.getOrNull() }
        .filter { it.second.isNotEmpty() }
        .toMap()
}