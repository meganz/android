package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default CloudExplorerNode search Nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getSearchFromMegaNodeParentUseCase [GetSearchFromMegaNodeParentUseCase]
 * @property getSearchInSharesNodesUseCase [GetSearchInSharesNodesUseCase]
 */
class IncomingExplorerSearchNodeUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getSearchFromMegaNodeParentUseCase: GetSearchFromMegaNodeParentUseCase,
    private val getSearchInSharesNodesUseCase: GetSearchInSharesNodesUseCase,
) {

    /**
     * Use Case which search Nodes in CloudExplorer
     * @param query Query to be searched
     * @param parentHandle
     * @param parentHandleSearch
     * @param searchType
     */
    suspend operator fun invoke(
        query: String?,
        parentHandle: Long,
        parentHandleSearch: Long,
        searchType: Int = -1
    ): List<MegaNode> {
        return query?.let {
            if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                getSearchInSharesNodesUseCase(query)
            }
            val parent = megaNodeRepository.getNodeByHandle(parentHandle)
            getSearchFromMegaNodeParentUseCase(
                query = query,
                parentHandleSearch = parentHandleSearch,
                parent = parent,
                searchType = searchType
            )
        } ?: run {
            emptyList()
        }
    }
}