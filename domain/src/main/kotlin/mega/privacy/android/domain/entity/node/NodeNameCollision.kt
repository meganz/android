package mega.privacy.android.domain.entity.node

import mega.privacy.android.domain.entity.pitag.PitagTrigger

/**
 * Node name collision
 *
 * @property renameName
 * @property nodeHandle
 */
sealed interface NodeNameCollision : NameCollision {
    val nodeHandle: Long

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
        override val renameName: String? = null,
        val type: NodeNameCollisionType = NodeNameCollisionType.COPY,
    ) : NodeNameCollision {
        override val pitagTrigger = PitagTrigger.NotApplicable
    }

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
        override val renameName: String? = null,
        val chatId: Long,
        val messageId: Long,
    ) : NodeNameCollision {
        override val pitagTrigger = PitagTrigger.NotApplicable
    }

    /**
     * Public file-link node name collision while importing. A file-link import is always a copy,
     * and the node is not in the account tree, so it can only be copied from its serialized form.
     *
     * @property serializedData the serialized public node
     */
    data class FileLink(
        override val collisionHandle: Long,
        override val nodeHandle: Long,
        override val name: String,
        override val size: Long,
        override val childFolderCount: Int,
        override val childFileCount: Int,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        override val renameName: String? = null,
        val serializedData: String,
    ) : NodeNameCollision {
        override val pitagTrigger = PitagTrigger.NotApplicable
    }
}
