package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Is available offline
 */
fun interface IsAvailableOffline {
    /**
     * Invoke
     *
     * @param node
     * @return true if the file is available offline and up to date
     */
    suspend operator fun invoke(node: TypedNode): Boolean
}