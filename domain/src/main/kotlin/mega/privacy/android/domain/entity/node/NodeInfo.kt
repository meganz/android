package mega.privacy.android.domain.entity.node

/**
 * Data class storing data for a node such as name and isNodeKeyDecrypted
 *
 * @property name Name of Node
 * @property isNodeKeyDecrypted Whether the node is decrypted
 */
data class NodeInfo(
    val name: String,
    val isNodeKeyDecrypted: Boolean,
)