package mega.privacy.android.domain.usecase.node.clouddrive

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.clouddrive.NodeFetchResult
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import javax.inject.Inject

/**
 * Use case for getting node children in chunks with progressive loading
 * @property nodeRepository The node repository
 * @property getFolderTypeDataUseCase The use case for getting folder type data
 * @property getCloudSortOrder The use case for getting cloud sort order
 * @property containsMediaItemUseCase The use case for checking if a node contains media items
 */
class FetchNodesByIdInChunkUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
) {

    /**
     * Get node children in chunks for progressive loading
     *
     * @param nodeId The parent node ID
     * @param initialBatchSize The initial batch size for loading nodes, default is 500
     * @return Flow that emits pairs containing typed node lists and hasMore flag progressively
     */
    suspend operator fun invoke(
        nodeId: NodeId,
        initialBatchSize: Int = 500,
    ): Flow<NodeFetchResult> = coroutineScope {
        val sortOrderDiffer = async { getCloudSortOrder() }
        val folderTypeDataDiffer = async { getFolderTypeDataUseCase() }
        nodeRepository.getTypedNodesByIdInChunks(
            nodeId = nodeId,
            order = sortOrderDiffer.await(),
            initialBatchSize = initialBatchSize,
            folderTypeData = folderTypeDataDiffer.await(),
        ).map { (nodes, hasMore) ->
            val hasMediaItems = containsMediaItemUseCase(nodes)
            NodeFetchResult(
                loadingState = if (hasMore) NodesLoadingState.PartiallyLoaded else NodesLoadingState.FullyLoaded,
                hasMediaItems = hasMediaItems,
                typedNodes = nodes
            )
        }.catch {
            emit(
                NodeFetchResult(
                    loadingState = NodesLoadingState.Failed,
                    hasMediaItems = false,
                    typedNodes = emptyList()
                )
            )
        }
    }
}