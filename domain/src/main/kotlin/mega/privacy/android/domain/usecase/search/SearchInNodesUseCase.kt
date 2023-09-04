package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to search in nodes for nodeID
 * @property addNodeType [AddNodeType]
 * @property searchRepository [SearchRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class SearchInNodesUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val searchRepository: SearchRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * invoke
     * @param nodeId [NodeId] where search should be performed
     * @param searchCategory [SearchCategory] filter type of search
     * @param query query to be searched
     */
    suspend operator fun invoke(
        nodeId: NodeId?,
        searchCategory: SearchCategory = SearchCategory.ALL,
        query: String,
    ): List<TypedNode> =
        searchRepository.search(
            nodeId = nodeId,
            query = query,
            searchCategory = searchCategory,
            order = getCloudSortOrder()
        ).map { addNodeType(it) }

}