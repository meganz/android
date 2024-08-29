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
 * @property renameName
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
    val renameName: String?
}

/**
 * Creates a copy of the NameCollision with a new renameName
 */
fun NameCollision.copy(renameName: String?): NameCollision {
    return when (this) {
        is NodeNameCollision.Default -> copy(renameName = renameName)
        is NodeNameCollision.Chat -> copy(renameName = renameName)
        is FileNameCollision -> copy(renameName = renameName)
    }
}