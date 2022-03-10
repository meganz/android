package mega.privacy.android.app.namecollision.data

import android.net.Uri
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import nz.mega.sdk.MegaNode
import java.io.File
import java.io.Serializable

/**
 * Data class containing all the required to create a [NameCollisionResult].
 *
 * @property collisionHandle        The node handle with which there is a name collision.
 * @property name                   The name of the item to upload, copy or move.
 * @property size                   The size of the item to upload, copy or move.
 * @property lastModified           The last modified date of the item to upload, copy or move.
 * @property parentHandle           The parent handle of the node in which the item has to be uploaded, copied or moved.
 * @property isFile                 True if the item is a file, false if is a folder.
 */
sealed class NameCollision : Serializable {
    abstract val collisionHandle: Long
    abstract val name: String
    abstract val size: Long
    abstract val lastModified: Long
    abstract val parentHandle: Long
    abstract val isFile: Boolean

    /**
     * Data class containing all the required to present an upload name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property absolutePath       The absolute path of the file to upload.
     * @property name               The name of the file to upload.
     * @property lastModified       The last modified date of the file to upload.
     * @property parentHandle       The parent handle of the node in which the file has to be uploaded.
     * @property isFile             True if the file is a file, false if is a folder.
     */
    data class Upload constructor(
        override val collisionHandle: Long,
        val absolutePath: String,
        override val name: String,
        override val size: Long,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean = true
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
            fun getUploadCollision(collisionHandle: Long, file: File, parentHandle: Long): Upload =
                Upload(
                    collisionHandle = collisionHandle,
                    absolutePath = file.absolutePath,
                    name = file.name,
                    size = file.length(),
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
                parentHandle: Long
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = shareInfo.fileAbsolutePath,
                name = shareInfo.originalFileName,
                size = shareInfo.size,
                lastModified = shareInfo.lastModified,
                parentHandle = parentHandle
            )

            /**
             * Gets a [NameCollision.Upload] from an [UploadFolderResult].
             *
             * @param collisionHandle       The node handle with which there is a name collision.
             * @param uploadFolderResult    The file from which the [NameCollision.Upload] will be get.
             */
            @JvmStatic
            fun getUploadCollision(
                collisionHandle: Long,
                uploadFolderResult: UploadFolderResult
            ): Upload = Upload(
                collisionHandle = collisionHandle,
                absolutePath = uploadFolderResult.absolutePath,
                name = uploadFolderResult.name,
                size = uploadFolderResult.size,
                lastModified = uploadFolderResult.lastModified,
                parentHandle = uploadFolderResult.parentHandle
            )
        }
    }

    /**
     * Data class containing all the required to present a copy name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property nodeHandle         The node handle of the node to copy.
     * @property name               The name of the node to copy.
     * @property lastModified       The last modified date of the node to copy.
     * @property parentHandle       The parent handle of the node in which the file has to be copied.
     * @property isFile             True if the node is a file, false if is a folder.
     */
    data class Copy constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean
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
            fun getCopyCollision(collisionHandle: Long, node: MegaNode, parentHandle: Long): Copy =
                Copy(
                    collisionHandle = collisionHandle,
                    nodeHandle = node.handle,
                    name = node.name,
                    size = node.size,
                    lastModified = node.modificationTime,
                    parentHandle = parentHandle,
                    isFile = node.isFile
                )
        }
    }

    /**
     * Data class containing all the required to present a movement name collision.
     *
     * @property collisionHandle    The node handle with which there is a name collision.
     * @property nodeHandle         The node handle of the node to move.
     * @property name               The name of the node to move.
     * @property lastModified       The last modified date of the node to move.
     * @property parentHandle       The parent handle of the node in which the file has to be moved.
     * @property isFile             True if the node is a file, false if is a folder.
     */
    data class Movement constructor(
        override val collisionHandle: Long,
        val nodeHandle: Long,
        override val name: String,
        override val size: Long,
        override val lastModified: Long,
        override val parentHandle: Long,
        override val isFile: Boolean
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
                parentHandle: Long
            ): Movement = Movement(
                collisionHandle = collisionHandle,
                nodeHandle = node.handle,
                name = node.name,
                size = node.size,
                lastModified = node.modificationTime,
                parentHandle = parentHandle,
                isFile = node.isFile
            )
        }
    }
}
