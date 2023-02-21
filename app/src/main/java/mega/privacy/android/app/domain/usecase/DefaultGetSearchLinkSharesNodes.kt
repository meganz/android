package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get Link share  nodes from searched Query
 *
 * @property megaNodeRepository [MegaNodeRepository]
 * @property getCloudSortOrder Sort order of cloud
 */
class DefaultGetSearchLinkSharesNodes @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) : GetSearchLinkSharesNodes {
    override suspend fun invoke(
        query: String,
        megaCancelToken: MegaCancelToken,
        isFirstLevel: Boolean,
    ): List<MegaNode> {
        return megaNodeRepository.searchLinkShares(
            query = query,
            megaCancelToken = megaCancelToken,
            order = getCloudSortOrder(),
            isFirstLevelNavigation = isFirstLevel
        )
    }
}