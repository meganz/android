package mega.privacy.android.shared.nodes.namecollision

/**
 * Info required to present the replace, update or merge option of a name collision.
 *
 * @property isFileVersioningEnabled    True if file versioning is enabled, false otherwise.
 * @property nameCollisionType          Collision type: upload, movement or copy.
 * @property isFile                     True if the collision is related to a file, false if it is to a folder.
 * @property canReplaceOrUpdate         True if the current user is allowed to replace or update the
 *                                      existing node, false otherwise.
 */
data class FileVersioningInfo(
    val isFileVersioningEnabled: Boolean,
    val nameCollisionType: NameCollisionType,
    val isFile: Boolean,
    val canReplaceOrUpdate: Boolean,
)
