package mega.privacy.android.app.namecollision.data

import android.content.Context
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.FileUtil.getFileFolderInfo
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import nz.mega.sdk.MegaNode
import java.io.File
import java.io.Serializable

/**
 * Data class containing all the required to create a [NameCollisionResult].
 *
 * @property collisionHandle        The node handle with which there is a name collision.
 * @property name                   The name of the item to upload, copy or move.
 * @property size                   The size of the item to upload, copy or move if is a file, null otherwise.
 * @property folderContent          The content of the item to upload, copy or move if is a folder, null otherwise.
 * @property lastModified           The last modified date of the item to upload, copy or move.
 * @property parentHandle           The parent handle of the node in which the item has to be uploaded, copied or moved.
 * @property isFile                 True if the item is a file, false if is a folder.
 */
sealed class NameCollision : Serializable {
    abstract val collisionHandle: Long
    abstract val name: String
    abstract val size: Long?
    abstract val folderContent: String?
    abstract val lastModified: Long
    abstract val parentHandle: Long?
    abstract val isFile: Boolean

    /**
     * Data class containing all the required to present an upload name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property absolutePath       The absolute path of the file to upload.
     * @property name               The name of the file to upload.
     * @property size               The size of the item to upload, copy or move if is a file, null otherwise.
     * @property folderContent      The content of the item to upload, copy or move if is a folder, null otherwise.
     * @property lastModified       The last modified date of the file to upload.
     * @property parentHandle       The parent handle of the node in which the file has to be uploaded.
     * @property isFile             True if the file is a file, false if is a folder.
     */
    data class Upload constructor(
        override val collisionHandle: Long,
        val absolutePath: String,
        override val name: String,
        override val size: Long? = null,
        override val folderContent: String? = null,
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
                context: Context,
            ): Upload =
                Upload(
                    collisionHandle = collisionHandle,
                    absolutePath = file.absolutePath,
                    name = file.name,
                    size = if (file.isFile) file.length() else null,
                    folderContent = if (file.isDirectory) getFileFolderInfo(
                        file,
                        context
                    ) else null,
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
                context: Context,
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = uploadContent.uri.toString(),
                name = uploadContent.name!!,
                size = if (uploadContent.isFolder) null else uploadContent.size,
                folderContent = if (uploadContent.isFolder) getFileFolderInfo(
                    uploadContent.document,
                    context
                ) else null,
                lastModified = uploadContent.lastModified,
                parentHandle = parentHandle,
                isFile = !uploadContent.isFolder
            )
        }
    }

    /**
     * Data class containing all the required to present a copy name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property nodeHandle         The node handle of the node to copy.
     * @property name               The name of the node to copy.
     * @property size               The size of the item to upload, copy or move if is a file, null otherwise.
     * @property folderContent      The content of the item to upload, copy or move if is a folder, null otherwise.
     * @property lastModified       The last modified date of the node to copy.
     * @property parentHandle       The parent handle of the node in which the file has to be copied.
     * @property isFile             True if the node is a file, false if is a folder.
     */
    data class Copy constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val folderContent: String? = null,
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
                context: Context,
            ): Copy =
                Copy(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    name = node.name,
                    size = if (node.isFile) node.size else null,
                    folderContent = if (node.isFolder) getMegaNodeFolderInfo(
                        node,
                        context
                    ) else null,
                    lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                    parentHandle = parentHandle,
                    isFile = node.isFile
                )
        }
    }

    /**
     * Data class containing all the required to present an import name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property nodeHandle         The node handle of the node to import.
     * @property chatId             The chat identifier where is the node to import.
     * @property messageId          The message identifier where is the node to import.
     * @property name               The name of the node to import.
     * @property size               The size of the node to import.
     * @property folderContent      Null as the node is a file.
     * @property lastModified       The last modified date of the node to import.
     * @property parentHandle       The parent handle of the node in which the file has to be imported.
     * @property isFile             True as the node is a file.
     */
    data class Import constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        val chatId: Long,
        val messageId: Long,
        override val name: String,
        override val size: Long? = null,
        override val folderContent: String? = null,
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
     * Data class containing all the required to present a movement name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property nodeHandle         The node handle of the node to move.
     * @property name               The name of the node to move.
     * @property size               The size of the item to upload, copy or move if is a file, null otherwise.
     * @property folderContent      The content of the item to upload, copy or move if is a folder, null otherwise.
     * @property lastModified       The last modified date of the node to move.
     * @property parentHandle       The parent handle of the node in which the file has to be moved.
     * @property isFile             True if the node is a file, false if is a folder.
     */
    data class Movement constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long? = null,
        override val folderContent: String? = null,
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
                context: Context,
            ): Movement = Movement(
                collisionHandle = collisionHandle,
                nodeHandle = node.handle,
                name = node.name,
                size = if (node.isFile) node.size else null,
                folderContent = if (node.isFolder) getMegaNodeFolderInfo(node, context) else null,
                lastModified = if (node.isFile) node.modificationTime else node.creationTime,
                parentHandle = parentHandle,
                isFile = node.isFile
            )
        }
    }
}
