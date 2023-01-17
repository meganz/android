package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting parent node by handle
 */
fun interface GetParentNodeByHandle {

    /**
     * Get parent node by handle
     *
     * @param parentHandle node handle
     * @return [UnTypedNode]?
     */
    suspend operator fun invoke(parentHandle: Long): UnTypedNode?
}