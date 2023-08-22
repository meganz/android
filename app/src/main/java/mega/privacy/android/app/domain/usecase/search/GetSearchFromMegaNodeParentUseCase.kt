package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for SearchFromMegaNodeParent search Nodes from searched Query for a [MegaNode]
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class GetSearchFromMegaNodeParentUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {

    /**
     * Use case for search query by MegaNode
     * @param query
     * @param parentHandleSearch
     * @param parent [MegaNode]
     * @param searchType
     */
    suspend operator fun invoke(
        query: String,
        parentHandleSearch: Long,
        parent: MegaNode?,
        searchType: Int,
    ): List<MegaNode>? {
        return parent?.let {
            if (query.isEmpty() || parentHandleSearch != MegaApiJava.INVALID_HANDLE) {
                megaNodeRepository.getChildrenNode(it, getCloudSortOrder())
            } else {
                megaNodeRepository.search(
                    parentNode = it,
                    query = query,
                    order = getCloudSortOrder(),
                    searchType = searchType
                )
            }
        } ?: run {
            emptyList()
        }
    }
}