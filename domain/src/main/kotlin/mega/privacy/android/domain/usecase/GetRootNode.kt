package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting root node
 */
fun interface GetRootNode {

    /**
     * Get root node
     *
     * @return root node
     */
    suspend operator fun invoke(): UnTypedNode?
}