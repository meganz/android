package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.AddNodesTypeUseCase
import javax.inject.Inject

/**
 * Use case to search in nodes for nodeID
 * @property addNodesTypeUseCase [AddNodesTypeUseCase]
 * @property searchRepository [SearchRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class SearchInNodesUseCase @Inject constructor(
    private val addNodesTypeUseCase: AddNodesTypeUseCase,
    private val searchRepository: SearchRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * invoke
     * @param nodeId [NodeId] where search should be performed
     * @param searchCategory [SearchCategory] filter type of search
     * @param query query to be searched
     * @param modificationDate modified date filter if set [DateFilterOption]
     * @param creationDate added date filter if set [DateFilterOption]
     */
    suspend operator fun invoke(
        nodeId: NodeId?,
        searchCategory: SearchCategory = SearchCategory.ALL,
        query: String,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
    ): List<TypedNode> = addNodesTypeUseCase(
        searchRepository.search(
            nodeId = nodeId,
            query = query,
            searchCategory = searchCategory,
            order = getCloudSortOrder(),
            modificationDate = modificationDate,
            creationDate = creationDate,
        )
    )
}
