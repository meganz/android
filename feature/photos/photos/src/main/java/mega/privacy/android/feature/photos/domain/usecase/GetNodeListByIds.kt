package mega.privacy.android.feature.photos.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Interface to get nodeList by ids
 */
@Deprecated("Replace with GetNodeListByIdsUseCase")
interface GetNodeListByIds {
    /**
     * Get nodeList by ids
     */
    suspend operator fun invoke(ids: List<Long>): List<MegaNode>
}