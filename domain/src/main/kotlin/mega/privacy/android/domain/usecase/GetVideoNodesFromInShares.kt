package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting video nodes from InShares
 */
fun interface GetVideoNodesFromInShares {

    /**
     * Getting video nodes from InShares
     *
     * @param order [SortOrder]
     * @return video nodes
     */
    suspend operator fun invoke(order: SortOrder): List<TypedNode>
}