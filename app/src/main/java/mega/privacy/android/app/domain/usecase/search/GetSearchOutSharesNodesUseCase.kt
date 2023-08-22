package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get Out share nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder Sort order of cloud
 */
class GetSearchOutSharesNodesUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * Invoke
     * @param query query to be searched
     */
    suspend operator fun invoke(query: String): List<MegaNode> {
        val searchList =
            megaNodeRepository.searchOutShares(query, getCloudSortOrder())
        return if (query.isEmpty()) {
            if (getCloudSortOrder() == SortOrder.ORDER_DEFAULT_DESC) {
                searchList.sortedWith(
                    compareByDescending<MegaNode> { it.isFolder }.reversed()
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }).reversed()
            } else {
                searchList.sortedWith(compareBy<MegaNode> { it.isFolder }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        } else {
            searchList
        }
    }
}