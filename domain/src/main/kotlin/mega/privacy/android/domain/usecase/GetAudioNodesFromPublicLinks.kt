package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for get audio nodes from public links
 */
interface GetAudioNodesFromPublicLinks {

    /**
     * Get audio nodes from public links
     *
     * @param order [SortOrder]
     * @return audio nodes
     */
    suspend operator fun invoke(order: SortOrder): List<TypedNode>
}