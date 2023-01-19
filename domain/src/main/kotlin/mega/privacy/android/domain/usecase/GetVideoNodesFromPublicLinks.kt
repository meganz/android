package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The implementation of [GetVideoNodesFromPublicLinks]
 */
interface GetVideoNodesFromPublicLinks {

    /**
     * Get video nodes from public links
     *
     * @param order [SortOrder]
     * @return video nodes
     */
    suspend operator fun invoke(order: SortOrder): List<TypedNode>
}