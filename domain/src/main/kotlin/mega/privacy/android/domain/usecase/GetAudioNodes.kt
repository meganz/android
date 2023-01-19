package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting audio nodes
 */
interface GetAudioNodes {

    /**
     * Getting audio nodes
     *
     * @param order [SortOrder]
     * @return audio nodes
     */
    suspend operator fun invoke(order: SortOrder): List<TypedNode>
}