package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case to get In share  nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder Sort order of cloud
 */
class GetSearchInSharesNodesUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder
) {

    /**
     * Get the Inshares mega nodes of searched Query
     * @param query: Query needs to searched
     */
    suspend operator fun invoke(
        query: String,
    ): List<MegaNode> {
        return megaNodeRepository.searchInShares(
            query = query,
            order = getCloudSortOrder()
        )
    }
}