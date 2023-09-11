package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.data.mapper.search.SearchCategoryIntMapper
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.search.SearchCategory
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
    private val searchCategoryIntMapper: SearchCategoryIntMapper,
) {

    /**
     * Use case for search query by MegaNode
     * @param query
     * @param parentHandleSearch
     * @param parent [MegaNode]
     * @param searchFilter [SearchFilter]
     */
    suspend operator fun invoke(
        query: String,
        parentHandleSearch: Long,
        parent: MegaNode?,
        searchFilter: SearchFilter?,
    ): List<MegaNode> {
        return parent?.let {
            if ((query.isEmpty() || parentHandleSearch != MegaApiJava.INVALID_HANDLE)
                && (searchFilter == null || searchFilter.filter == SearchCategory.ALL)
            ) {
                megaNodeRepository.getChildrenNode(it, getCloudSortOrder())
            } else {
                val searchFilterType =
                    searchCategoryIntMapper(searchFilter?.filter ?: SearchCategory.ALL)
                megaNodeRepository.search(
                    parentNode = it,
                    query = query,
                    order = getCloudSortOrder(),
                    searchType = searchFilterType
                )
            }
        } ?: run {
            emptyList()
        }
    }
}