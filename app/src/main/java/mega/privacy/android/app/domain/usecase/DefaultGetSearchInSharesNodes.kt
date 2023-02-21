package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get In share  nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder Sort order of cloud
 */
class DefaultGetSearchInSharesNodes @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder
) : GetSearchInSharesNodes {
    override suspend fun invoke(
        query: String,
        megaCancelToken: MegaCancelToken,
    ): List<MegaNode> {
        return megaNodeRepository.searchInShares(
            query = query,
            megaCancelToken = megaCancelToken,
            order = getCloudSortOrder()
        )
    }
}