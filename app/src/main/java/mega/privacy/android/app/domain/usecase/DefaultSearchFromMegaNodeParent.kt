package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default SearchFromMegaNodeParent search Nodes from searched Query for a [MegaNode]
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder [GetCloudSortOrder]
 */
class DefaultSearchFromMegaNodeParent @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) : GetSearchFromMegaNodeParent {
    override suspend fun invoke(
        query: String,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
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
                    megaCancelToken = megaCancelToken,
                    searchType = searchType
                )
            }
        } ?: run {
            emptyList()
        }
    }
}