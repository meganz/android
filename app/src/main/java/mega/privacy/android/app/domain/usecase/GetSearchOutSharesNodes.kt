package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Get the Outshares mega nodes of searched Query
 */
interface GetSearchOutSharesNodes {
    /**
     * Get the Inshares mega nodes of searched Query
     * @param query: Query needs to searched
     * @param megaCancelToken [MegaCancelToken]
     */
    suspend operator fun invoke(query: String, megaCancelToken: MegaCancelToken): List<MegaNode>
}