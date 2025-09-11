package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import javax.inject.Inject

/**
 * Use case for getting node children in chunks with progressive loading
 */
class GetNodesByIdInChunkUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
) {

    /**
     * Get node children in chunks for progressive loading
     *
     * @param nodeId The parent node ID
     * @param initialBatchSize The initial batch size for loading nodes, default is 1000
     * @return Flow that emits pairs containing typed node lists and hasMore flag progressively
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        initialBatchSize: Int = 1000,
    ): Flow<Pair<List<TypedNode>, Boolean>> = nodeRepository.getNodeChildrenInChunks(
        nodeId = nodeId,
        order = getCloudSortOrder(),
        initialBatchSize = initialBatchSize,
        folderTypeData = getFolderTypeDataUseCase(),
    )
}