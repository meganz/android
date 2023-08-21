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
     * @param searchType
     */
    suspend operator fun invoke(
        query: String,
        parentHandleSearch: Long,
        megaCancelToken: MegaCancelToken,
        parent: MegaNode?,
        searchType: Int = -1
    ): List<MegaNode>?
}