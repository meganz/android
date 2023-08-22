package mega.privacy.android.app.domain.usecase.search

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Get the Outshares mega nodes of searched Query
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder Sort order of cloud
 */
class GetSearchLinkSharesNodesUseCase @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * Invoke
     * @param query query to be searched
     * @param isFirstLevel isOnFirstLevel
     */
    suspend operator fun invoke(
        query: String,
        isFirstLevel: Boolean,
    ): List<MegaNode> {
        return megaNodeRepository.searchLinkShares(
            query = query,
            order = getCloudSortOrder(),
            isFirstLevelNavigation = isFirstLevel
        )
    }
}