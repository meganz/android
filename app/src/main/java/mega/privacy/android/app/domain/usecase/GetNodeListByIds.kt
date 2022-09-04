package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Interface to get nodeList by ids
 */
interface GetNodeListByIds {
    /**
     * Get nodeList by ids
     */
    suspend operator fun invoke(ids: List<Long>): List<MegaNode>
}
