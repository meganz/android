package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Get the Outshares mega nodes of searched Query
 */
interface GetSearchLinkSharesNodes {
    /**
     * Get the LinksShares mega nodes of searched Query
     * @param query: Query needs to searched
     * @param megaCancelToken [MegaCancelToken]
     * @param isFirstLevel
     */
    suspend operator fun invoke(
        query: String,
        megaCancelToken: MegaCancelToken,
        isFirstLevel: Boolean,
    ): List<MegaNode>
}