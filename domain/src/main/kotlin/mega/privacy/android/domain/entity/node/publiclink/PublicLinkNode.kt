package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Public link node
 *
 * @constructor Create empty Public link node
 */
sealed interface PublicLinkNode : TypedNode {
    /**
     * Parent node
     */
    val parent: PublicLinkFolder?
}