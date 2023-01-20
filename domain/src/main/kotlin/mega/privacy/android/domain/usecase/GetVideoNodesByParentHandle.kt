package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting video children by parent node handle
 */
interface GetVideoNodesByParentHandle {

    /**
     * Get video children by parent node handle
     *
     * @param parentHandle parent node handle
     * @param order list order
     * @return video nodes
     */
    suspend operator fun invoke(parentHandle: Long, order: SortOrder): List<TypedNode>?
}