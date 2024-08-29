package mega.privacy.android.app.namecollision.data

import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity.Upload.Companion.toFileNameCollision
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.uri.UriPath
import java.io.Serializable

/**
 * Name collision UI entity
 * It's also used when data needs to be transferred between activities via intent
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
 * @constructor Create empty Name collision
 */
sealed class NameCollisionUiEntity : Serializable {
    abstract val collisionHandle: Long
    abstract val name: String
    abstract val size: Long?
    abstract val childFolderCount: Int
    abstract val childFileCount: Int
    abstract val lastModified: Long
    abstract val parentHandle: Long?
    abstract val isFile: Boolean
    abstract val renameName: String?

    /**
     * Upload
     *
     * @property absolutePath
     */
    data class Upload(
        override val collisionHandle: Long,
        val absolutePath: String,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long?,
        override val isFile: Boolean = true,
        override val renameName: String? = null,
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Get upload collision
             *
             * @param collision The [FileNameCollision] from which the [NameCollisionUiEntity.Upload] will be get.
             * @return
             */
            fun fromFileNameCollision(
                collision: FileNameCollision,
            ): Upload = Upload(
                collisionHandle = collision.collisionHandle,
                absolutePath = collision.path.value,
                name = collision.name,
                size = if (collision.isFile) collision.size else null,
                childFolderCount = collision.childFolderCount,
                childFileCount = collision.childFileCount,
                lastModified = collision.lastModified,
                parentHandle = collision.parentHandle,
                isFile = collision.isFile,
                renameName = collision.renameName
            )

            /**
             * Creates a [FileNameCollision] from a [NameCollisionUiEntity.Upload]
             */
            fun Upload.toFileNameCollision() = FileNameCollision(
                collisionHandle = collisionHandle,
                name = name,
                size = size ?: 0L,
                childFolderCount = childFileCount,
                childFileCount = childFileCount,
                lastModified = lastModified,
                parentHandle = parentHandle ?: -1L,
                isFile = isFile,
                renameName = renameName,
                path = UriPath(absolutePath)
            )
        }
    }

    /**
     * Copy
     *
     * @property nodeHandle
     * @property serializedNode
     */
    data class Copy(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        override val renameName: String? = null,
        val serializedNode: String? = null,
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Copy] from a [NodeNameCollision].
             */
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision,
            ): Copy = Copy(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile,
                renameName = nameCollision.renameName,
                serializedNode = nameCollision.serializedData
            )
        }

        /**
         * Creates a [NodeNameCollision.Default] from a [NameCollisionUiEntity.Copy]
         */
        fun Copy.toNodeNameCollision() = NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFolderCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = serializedNode,
            renameName = renameName,
            type = NodeNameCollisionType.COPY
        )
    }

    /**
     * Import
     *
     * @property nodeHandle
     * @property chatId
     * @property messageId
     */
    data class Import(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        val chatId: Long,
        val messageId: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean = true,
        override val renameName: String? = null,
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Import] from a [NodeNameCollision.Chat].
             */
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision.Chat,
            ): Import = Import(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                chatId = nameCollision.chatId,
                messageId = nameCollision.messageId,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile,
                renameName = nameCollision.renameName
            )
        }

        /**
         * Creates a [NodeNameCollision.Chat] from a [NameCollisionUiEntity.Import].
         */
        fun Import.toNodeNameCollision() = NodeNameCollision.Chat(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFolderCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = renameName,
            chatId = chatId,
            messageId = messageId
        )
    }

    /**
     * Movement
     *
     * @property nodeHandle
     */
    data class Movement(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
        override val renameName: String? = null,
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Movement] from a [NodeNameCollision].
             */
            fun fromNodeNameCollision(
                nameCollision: NodeNameCollision,
            ): Movement = Movement(
                collisionHandle = nameCollision.collisionHandle,
                nodeHandle = nameCollision.nodeHandle,
                name = nameCollision.name,
                size = nameCollision.size,
                childFolderCount = nameCollision.childFolderCount,
                childFileCount = nameCollision.childFileCount,
                lastModified = nameCollision.lastModified,
                parentHandle = nameCollision.parentHandle,
                isFile = nameCollision.isFile,
                renameName = nameCollision.renameName
            )
        }

        /**
         * Creates a [NodeNameCollision.Default] from a [NameCollisionUiEntity.Movement].
         */
        fun Movement.toNodeNameCollision() = NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFolderCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = renameName,
            type = NodeNameCollisionType.MOVE
        )
    }
}


/**
 * Temporary mapper to create a domain module's [mega.privacy.android.domain.entity.node.NameCollision] from app module's [NameCollisionUiEntity]
 * Should be removed when all features are refactored
 */
fun NameCollisionUiEntity.toDomainEntity(): NameCollision =
    when (this) {
        is NameCollisionUiEntity.Copy -> this.toNodeNameCollision()

        is NameCollisionUiEntity.Movement -> this.toNodeNameCollision()

        is NameCollisionUiEntity.Import -> this.toNodeNameCollision()

        is NameCollisionUiEntity.Upload -> this.toFileNameCollision()
    }

/**
 * Creates [NameCollisionUiEntity] from domain module's [NameCollision]
 */
fun NameCollision.toUiEntity() = when (this) {
    is NodeNameCollision.Default -> when (type) {
        NodeNameCollisionType.MOVE -> NameCollisionUiEntity.Movement.fromNodeNameCollision(this)
        else -> NameCollisionUiEntity.Copy.fromNodeNameCollision(this)
    }

    is NodeNameCollision.Chat -> NameCollisionUiEntity.Import.fromNodeNameCollision(this)
    is FileNameCollision -> NameCollisionUiEntity.Upload.fromFileNameCollision(this)
}