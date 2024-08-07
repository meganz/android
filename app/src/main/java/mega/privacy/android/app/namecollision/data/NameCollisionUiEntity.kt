package mega.privacy.android.app.namecollision.data

import mega.privacy.android.app.ShareInfo
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.uri.UriPath
import java.io.File
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
    ) : NameCollisionUiEntity() {

        companion object {

            /**
             * Gets a [NameCollisionUiEntity.Upload] from a [File].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param file              The file from which the [NameCollisionUiEntity.Upload] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be uploaded.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                file: File,
                parentHandle: Long?,
            ): Upload =
                Upload(
                    collisionHandle = collisionHandle,
                    absolutePath = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else null,
                    childFolderCount = file.listFiles()?.count { it.isDirectory } ?: 0,
                    childFileCount = file.listFiles()?.count { it.isFile } ?: 0,
                    lastModified = file.lastModified(),
                    parentHandle = parentHandle
                )

            /**
             * Gets a [NameCollisionUiEntity.Upload] from a [ShareInfo].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param shareInfo         The file from which the [NameCollisionUiEntity.Upload] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be uploaded.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                shareInfo: ShareInfo,
                parentHandle: Long,
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = shareInfo.fileAbsolutePath,
                name = shareInfo.originalFileName,
                size = shareInfo.size,
                lastModified = shareInfo.lastModified,
                parentHandle = parentHandle
            )

            /**
             * Get upload collision
             *
             * @param collision The [FileNameCollision] from which the [NameCollisionUiEntity.Upload] will be get.
             * @return
             */
            @JvmStatic
            fun getUploadCollision(
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
                isFile = collision.isFile
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
        val serializedNode: String? = null,
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Copy] from a [NodeNameCollision].
             */
            @JvmStatic
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
                serializedNode = nameCollision.serializedData
            )
        }
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
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Import] from a [NodeNameCollision.Chat].
             */
            @JvmStatic
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
                isFile = nameCollision.isFile
            )
        }
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
    ) : NameCollisionUiEntity() {

        companion object {
            /**
             * Creates a [NameCollisionUiEntity.Movement] from a [NodeNameCollision].
             */
            @JvmStatic
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
                isFile = nameCollision.isFile
            )
        }
    }
}

/**
 * Temporary mapper to create a [NameCollisionUiEntity.Import] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.Chat.toLegacyImport() =
    NameCollisionUiEntity.Import.fromNodeNameCollision(this)

/**
 * Temporary mapper to create a [NameCollisionUiEntity.Movement] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.toLegacyMove() = NameCollisionUiEntity.Movement.fromNodeNameCollision(this)

/**
 * Temporary mapper to create a [NameCollisionUiEntity.Copy] from domain module's [NodeNameCollision]
 * Should be removed when [GetNameCollisionResultUseCase] is refactored
 */
fun NodeNameCollision.toLegacyCopy() = NameCollisionUiEntity.Copy.fromNodeNameCollision(this)


/**
 * Temporary mapper to create a domain module's [mega.privacy.android.domain.entity.node.NameCollision] from app module's [NameCollisionUiEntity]
 * Should be removed when all features are refactored
 */
fun NameCollisionUiEntity.toDomainEntity(): NameCollision =
    when (this) {
        is NameCollisionUiEntity.Copy -> NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = serializedNode,
            renameName = null,
            type = NodeNameCollisionType.COPY
        )

        is NameCollisionUiEntity.Movement -> NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = null,
            type = NodeNameCollisionType.MOVE
        )

        is NameCollisionUiEntity.Import -> NodeNameCollision.Chat(
            collisionHandle = collisionHandle,
            nodeHandle = nodeHandle,
            name = name,
            size = size ?: -1L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle,
            isFile = isFile,
            serializedData = null,
            renameName = null,
            chatId = chatId,
            messageId = messageId
        )

        is NameCollisionUiEntity.Upload -> FileNameCollision(
            collisionHandle = collisionHandle,
            name = name,
            size = size ?: 0L,
            childFolderCount = childFileCount,
            childFileCount = childFileCount,
            lastModified = lastModified,
            parentHandle = parentHandle ?: -1L,
            isFile = isFile,
            path = UriPath(absolutePath)
        )
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
    is FileNameCollision -> NameCollisionUiEntity.Upload.getUploadCollision(this)
}