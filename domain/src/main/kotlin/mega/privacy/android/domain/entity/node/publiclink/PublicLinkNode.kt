package mega.privacy.android.domain.entity.node.publiclink

/**
 * Public link node
 *
 * @constructor Create empty Public link node
 */
sealed interface PublicLinkNode {
    /**
     * Parent node
     */
    val parent: PublicLinkFolder?
}