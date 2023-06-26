package mega.privacy.android.domain.entity.node

/**
 * Node name collision type
 *
 */
enum class NodeNameCollisionType {
    /**
     * Restore node from rubbish bin
     */
    RESTORE,

    /**
     * Move node
     */
    MOVE
}