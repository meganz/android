package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting inbox node
 */
fun interface GetInboxNode {

    /**
     * Get inbox node
     *
     * @return inbox node
     */
    suspend operator fun invoke(): UnTypedNode?
}