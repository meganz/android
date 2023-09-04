package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to search nodes in incoming shares
 * @property addNodeType [AddNodeType]
 * @property searchRepository [SearchRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class IncomingSharesTabSearchUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val searchRepository: SearchRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {

    /**
     * invoke
     * @param query querry to be searched
     */
    suspend operator fun invoke(query: String): List<TypedNode> {
        val list = searchRepository.searchInShares(
            query = query,
            order = getCloudSortOrder()
        )
        return list.map { addNodeType(it) }
    }
}