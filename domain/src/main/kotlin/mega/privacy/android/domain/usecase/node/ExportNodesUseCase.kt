package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Use Case to Export Nodes
 */
class ExportNodesUseCase @Inject constructor(
    private val exportNodeUseCase: ExportNodeUseCase,
) {
    /**
     * Export Nodes referenced by their NodeHandle
     *
     * @param nodes List of NodeHandles to Export
     * @return Map of NodeHandle to the link if the request finished with success
     */
    suspend operator fun invoke(
        nodes: List<Long>,
    ): Map<Long, String> = supervisorScope {
        nodes.map { nodeHandle ->
            async {
                runCatching {
                    nodeHandle to exportNodeUseCase(NodeId(nodeHandle))
                }
            }
        }
    }.awaitAll().mapNotNull { it.getOrNull() }.toMap()

}