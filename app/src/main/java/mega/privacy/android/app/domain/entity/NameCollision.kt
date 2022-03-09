package mega.privacy.android.app.domain.entity

import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.uploadFolder.list.data.UploadFolderResult
import java.io.File
import java.io.Serializable

/**
 * Data class containing all the required to present a name collision.
 *
 * @property collisionHandle    The node handle with which there is a name collision.
 * @property absolutePath       The absolute path of the file to upload.
 *                              It can be null if the collision type is not UPLOAD.
 * @property nodeHandle         The node handle of the node to copy or move.
 *                              It can be null if the collision type is UPLOAD.
 * @property name               The name of the file to upload.
 * @property lastModified       The last modified date of the file to upload.
 * @property parentHandle       The parent handle of the node in which the file has to be uploaded.
 */
data class NameCollision(
    val collisionHandle: Long,
    val absolutePath: String? = null,
    val nodeHandle: Long? = null,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val parentHandle: Long,
    val isFile: Boolean = true,
    val type: Type
) : Serializable {

    /**
     * Constructor for file Upload Collisions, from a File.
     */
    constructor(
        collisionHandle: Long,
        file: File,
        parentHandle: Long,
        type: Type
    ) : this(
        collisionHandle,
        file.absolutePath,
        null,
        file.name,
        file.length(),
        file.lastModified(),
        parentHandle,
        true,
        type
    )

    /**
     * Constructor for file Upload Collisions, from a ShareInfo.
     */
    constructor(
        collisionHandle: Long,
        shareInfo: ShareInfo,
        parentHandle: Long,
        type: Type
    ) : this(
        collisionHandle,
        shareInfo.fileAbsolutePath,
        null,
        shareInfo.originalFileName,
        shareInfo.size,
        shareInfo.lastModified,
        parentHandle,
        true,
        type
    )

    /**
     * Constructor for file Upload Collisions, from a UploadFolderResult.
     */
    constructor(
        collisionHandle: Long,
        uploadFolderResult: UploadFolderResult,
        type: Type
    ) : this(
        collisionHandle,
        uploadFolderResult.absolutePath,
        null,
        uploadFolderResult.name,
        uploadFolderResult.size,
        uploadFolderResult.lastModified,
        uploadFolderResult.parentHandle,
        true,
        type
    )

    /**
     * Name collision type.
     */
    enum class Type {
        UPLOAD, MOVE, COPY
    }
}
