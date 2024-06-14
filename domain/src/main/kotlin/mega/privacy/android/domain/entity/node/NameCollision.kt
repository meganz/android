package mega.privacy.android.domain.entity.node

/**
 * Node name collision
 *
 * @property collisionHandle
 * @property name
 * @property size
 * @property childFolderCount
 * @property childFileCount
 * @property lastModified
 * @property parentHandle
 * @property isFile
 */
sealed interface NameCollision {
    val collisionHandle: Long
    val name: String
    val size: Long
    val childFolderCount: Int
    val childFileCount: Int
    val lastModified: Long
    val parentHandle: Long
    val isFile: Boolean
}