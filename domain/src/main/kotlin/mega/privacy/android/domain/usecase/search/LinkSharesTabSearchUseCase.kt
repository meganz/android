package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to search nodes in link shares
 * @property addNodeType [AddNodeType]
 * @property searchRepository [SearchRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class LinkSharesTabSearchUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val searchRepository: SearchRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * invoke
     * @param query query to be searched
     * @param isFirstLevel
     */
    suspend operator fun invoke(query: String, isFirstLevel: Boolean): List<TypedNode> {
        val list = searchRepository.searchLinkShares(
            query = query,
            order = getCloudSortOrder(),
            isFirstLevelNavigation = isFirstLevel
        )
        return list.map { addNodeType(it) }
    }
}