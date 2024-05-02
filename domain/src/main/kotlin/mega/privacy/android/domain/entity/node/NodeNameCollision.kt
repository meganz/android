package mega.privacy.android.domain.entity.node

/**
 * Node name collision
 *
 * @property collisionHandle
 * @property nodeHandle
 * @property name
 * @property size
 * @property childFolderCount
 * @property childFileCount
 * @property lastModified
 * @property parentHandle
 * @property isFile
 * @property serializedData
 * @property renameName
 */
sealed interface NodeNameCollision {
    val collisionHandle: Long
    val nodeHandle: Long
    val name: String
    val size: Long
    val childFolderCount: Int
    val childFileCount: Int
    val lastModified: Long
    val parentHandle: Long
    val isFile: Boolean
    val serializedData: String?
    val renameName: String?

    /**
     * General node name collision
     */
    data class Default(
        override val collisionHandle: Long,
        override val nodeHandle: Long,
        override val name: String,
        override val size: Long,
        override val childFolderCount: Int,
        override val childFileCount: Int,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        override val serializedData: String? = null,
        override val renameName: String? = null,
    ) : NodeNameCollision

    /**
     * Chat node name collision while importing
     *
     * @property chatId
     * @property messageId
     */
    data class Chat(
        override val collisionHandle: Long,
        override val nodeHandle: Long,
        override val name: String,
        override val size: Long,
        override val childFolderCount: Int,
        override val childFileCount: Int,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        override val serializedData: String? = null,
        override val renameName: String? = null,
        val chatId: Long,
        val messageId: Long,
    ) : NodeNameCollision
}