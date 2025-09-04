package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case for getting node children in chunks with progressive loading
 */
class GetNodesByIdInChunkUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodesTypeUseCase: AddNodesTypeUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
) {

    /**
     * Get node children in chunks for progressive loading
     *
     * @param nodeId The parent node ID
     * @param initialBatchSize The initial batch size for loading nodes, default is 1000
     * @return Flow that emits pairs containing node lists and hasMore flag progressively
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        initialBatchSize: Int = 1000,
    ) = nodeRepository.getNodeChildrenInChunks(
        nodeId = nodeId,
        order = getCloudSortOrder(),
        initialBatchSize = initialBatchSize,
    ).map { (nodes, hasMore) ->
        addNodesTypeUseCase(nodes) to hasMore
    }
}
