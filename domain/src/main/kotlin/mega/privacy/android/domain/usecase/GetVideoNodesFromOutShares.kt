package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting video nodes from out shares
 */
interface GetVideoNodesFromOutShares {

    /**
     * Getting video nodes from out shares
     *
     * @param order list order
     * @return video nodes
     */
    suspend operator fun invoke(lastHandle: Long, order: SortOrder): List<TypedNode>
}