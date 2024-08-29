package mega.privacy.android.domain.entity.node

/**
 * Node name collision
 *
 * @property serializedData
 * @property renameName
 * @property nodeHandle
 */
sealed interface NodeNameCollision : NameCollision {
    val nodeHandle: Long
    val serializedData: String?

    /**
     * General node name collision
     * @param type [NodeNameCollisionType]
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
        val type: NodeNameCollisionType = NodeNameCollisionType.COPY
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