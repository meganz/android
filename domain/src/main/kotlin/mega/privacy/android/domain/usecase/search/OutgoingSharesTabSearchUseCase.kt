package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.SearchRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * Use case to search nodes in outgoing shares
 * @property addNodeType [AddNodeType]
 * @property searchRepository [SearchRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class OutgoingSharesTabSearchUseCase @Inject constructor(
    private val addNodeType: AddNodeType,
    private val searchRepository: SearchRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * invoke
     * @param query query to be searched
     */
    suspend operator fun invoke(query: String): List<TypedNode> {
        val list = searchRepository.searchOutShares(
            query = query,
            order = getCloudSortOrder()
        )
        return list.sortedWith(
            if (getCloudSortOrder() == SortOrder.ORDER_DEFAULT_DESC) {
                compareByDescending<UnTypedNode> { it is FolderNode }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            } else {
                compareBy<UnTypedNode> { it is FolderNode }.thenBy(
                    String.CASE_INSENSITIVE_ORDER
                ) { it.name }
            }
        ).map { addNodeType(it) }
    }
}