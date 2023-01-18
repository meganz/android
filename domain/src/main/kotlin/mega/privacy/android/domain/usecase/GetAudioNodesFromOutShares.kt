package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting audio nodes from out shares
 */
interface GetAudioNodesFromOutShares {

    /**
     * Getting audio nodes from out shares
     *
     * @param order list order
     * @return audio nodes
     */
    suspend operator fun invoke(lastHandle: Long, order: SortOrder): List<TypedNode>
}