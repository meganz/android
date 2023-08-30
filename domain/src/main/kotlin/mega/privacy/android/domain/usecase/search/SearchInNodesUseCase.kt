package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to search in nodes for nodeID
 * @property addNodeType [AddNodeType]
 * @property nodeRepository [NodeRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class SearchInNodesUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val nodeRepository: NodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * invoke
     * @param nodeId [NodeId] where search should be performed
     * @param searchType filter type of search
     * @param query query to be searched
     */
    suspend operator fun invoke(
        nodeId: NodeId?,
        searchType: Int = -1,
        query: String,
    ): List<TypedNode> =
        nodeRepository.search(
            nodeId = nodeId,
            query = query,
            searchType = searchType,
            order = getCloudSortOrder()
        ).map { addNodeType(it) }

}