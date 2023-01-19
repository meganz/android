package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting [UnTypedNode] by handle
 */
fun interface GetUnTypedNodeByHandle {

    /**
     * Get [UnTypedNode] by handle
     *
     * @param handle node handle
     * @return [UnTypedNode]?
     */
    suspend operator fun invoke(handle: Long): UnTypedNode?
}