package mega.privacy.android.app.domain.entity

import java.io.Serializable

/**
 * Data class containing all the required info to start the upload of a folder's item.
 *
 * @property collisionHandle    The node handle with which there is a name collision.
 * @property absolutePath       The absolute path of the file to upload.
 * @property name               The name of the file to upload.
 * @property lastModified       The last modified date of the file to upload.
 * @property parentHandle       The parent handle of the node in which the file has to be uploaded.
 */
data class NameCollision(
    val collisionHandle: Long,
    val absolutePath: String,
    val name: String,
    val lastModified: Long,
    val parentHandle: Long
) : Serializable
