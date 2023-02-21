package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode

/**
 * Use case for search query by MegaNode
 */
interface GetSearchFromMegaNodeParent {
    /**
     * Use case for search query by MegaNode
     * @param query
     * @param parentHandleSearch
     * @param megaCancelToken
     * @param parent [MegaNode]
     */
    suspend operator fun invoke(
        query: String,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
        parent: MegaNode?
    ): List<MegaNode>?
}