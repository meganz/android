package mega.privacy.android.app.namecollision.data

import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.domain.entity.node.NodeNameCollision
import nz.mega.sdk.MegaNode
import java.io.File
import java.io.Serializable

/**
 * Name collision
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
sealed class NameCollision : Serializable {
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
     * @property collisionHandle
     * @property absolutePath
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @constructor Create empty Upload
     */
    data class Upload constructor(
        override val collisionHandle: Long,
        val absolutePath: String,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long?,
        override val isFile: Boolean = true,
    ) : NameCollision() {

        companion object {

            /**
             * Gets a [NameCollision.Upload] from a [File].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param file              The file from which the [NameCollision.Upload] will be get.
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
             * Gets a [NameCollision.Upload] from a [ShareInfo].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param shareInfo         The file from which the [NameCollision.Upload] will be get.
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
             * Gets a [NameCollision.Upload] from an [FolderContent.Data].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param uploadContent     The file from which the [NameCollision.Upload] will be get.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                uploadContent: FolderContent.Data,
                parentHandle: Long,
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = uploadContent.uri.toString(),
                name = uploadContent.name!!,
                size = if (uploadContent.isFolder) null else uploadContent.size,
                childFolderCount = uploadContent.document.listFiles().count { it.isDirectory },
                childFileCount = uploadContent.document.listFiles().count { it.isFile },
                lastModified = uploadContent.lastModified,
                parentHandle = parentHandle,
                isFile = !uploadContent.isFolder
            )
        }
    }

    /**
     * Copy
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
     * @constructor Create empty Copy
     */
    data class Copy constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
    ) : NameCollision() {

        companion object {

            /**
             * Gets a [NameCollision.Copy] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param node              The node from which the [NameCollision.Copy] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be copied.
             */
            @JvmStatic
            fun getCopyCollision(
                collisionHandle: Long,
                node: MegaNode,
                parentHandle: Long,
                childFolderCount: Int,
                childFileCount: Int,
            ): Copy =
                Copy(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    name = node.name,
                    size = if (node.isFile) node.size else null,
                    childFolderCount = childFolderCount,
                    childFileCount = childFileCount,
                    lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                    parentHandle = parentHandle,
                    isFile = node.isFile
                )
        }
    }

    /**
     * Import
     *
     * @property collisionHandle
     * @property nodeHandle
     * @property chatId
     * @property messageId
     * @property name
     * @property size
     * @property childFolderCount
     * @property childFileCount
     * @property lastModified
     * @property parentHandle
     * @property isFile
     * @constructor Create empty Import
     */
    data class Import constructor(
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
    ) : NameCollision() {

        companion object {

            /**
             * Gets a [NameCollision.Import] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @property nodeHandle     The node handle of the node to import.
             * @property chatId         The chat identifier where is the node to import.
             * @property messageId      The message identifier where is the node to import.
             * @param node              The node from which the [NameCollision.Import] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be copied.
             */
            @JvmStatic
            fun getImportCollision(
                collisionHandle: Long,
                chatId: Long,
                messageId: Long,
                node: MegaNode,
                parentHandle: Long,
            ): Import =
                Import(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    chatId = chatId,
                    messageId = messageId,
                    name = node.name,
                    size = node.size,
                    lastModified = node.modificationTime,
                    parentHandle = parentHandle
                )
        }
    }

    /**
     * Movement
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
     * @constructor Create empty Movement
     */
    data class Movement constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val childFolderCount: Int = 0,
        override val childFileCount: Int = 0,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean,
    ) : NameCollision() {

        companion object {

            /**
             * Gets a [NameCollision.Movement] from a [MegaNode].
             *
             * @param collisionHandle   The node handle with which there is a name collision.
             * @param node              The node from which the [NameCollision.Movement] will be get.
             * @param parentHandle      The parent handle of the node in which the file has to be moved.
             */
            @JvmStatic
            fun getMovementCollision(
                collisionHandle: Long,
                node: MegaNode,
                parentHandle: Long,
                childFolderCount: Int,
                childFileCount: Int,
            ): Movement = Movement(
                collisionHandle = collisionHandle,
                nodeHandle = node.handle,
                name = node.name,
                size = if (node.isFile) node.size else null,
                childFolderCount = childFolderCount,
                childFileCount = childFileCount,
                lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                parentHandle = parentHandle,
                isFile = node.isFile
            )

            @JvmStatic
            fun getMovementCollision(
                nameCollision: NodeNameCollision
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
