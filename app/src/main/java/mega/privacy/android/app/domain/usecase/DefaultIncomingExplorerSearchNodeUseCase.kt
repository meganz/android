package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default CloudExplorerNode search Nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getSearchFromMegaNodeParent [GetSearchFromMegaNodeParent]
 * @property getSearchFromMegaNodeParent [GetSearchInSharesNodes]
 */
class DefaultIncomingExplorerSearchNodeUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent,
    private val getSearchInSharesNodes: GetSearchInSharesNodes,
) : GetIncomingExplorerSearchNodeUseCase {
    override suspend fun invoke(
        query: String?,
        parentHandle: Long,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode>? {
        return query?.let {
            if (parentHandle == MegaApiJava.INVALID_HANDLE) {
                getSearchInSharesNodes(query, megaCancelToken)
            }
            val parent = megaNodeRepository.getNodeByHandle(parentHandle)
            getSearchFromMegaNodeParent(
                query = query,
                parentHandleSearch = parentHandleSearch,
                parent = parent,
                megaCancelToken = megaCancelToken
            )
        } ?: run {
            emptyList()
        }
    }
}