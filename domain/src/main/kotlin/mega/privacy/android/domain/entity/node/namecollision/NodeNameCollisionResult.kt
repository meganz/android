package mega.privacy.android.domain.entity.node.namecollision

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * NodeNameCollisionResult
 * @property nameCollision The name collision
 * @property collisionName The node name with which there is a name collision
 * @property collisionSize The node size with which there is a name collision if is a file, null otherwise
 * @property collisionFolderContent The folder content of the node with which there is a name collision if is a folder, null otherwise
 * @property collisionLastModified The node last modified date with which there is a name collision
 * @property collisionThumbnail The node thumbnail if exists
 * @property thumbnail The thumbnail of the item to upload, copy or move if exists
 * @property choice The collision resolution
 */
@Serializable
data class NodeNameCollisionResult(
    val nameCollision: NameCollision,
    val collisionName: String? = null,
    val collisionSize: Long? = null,
    val collisionFolderContent: FolderTreeInfo? = null,
    val collisionLastModified: Long? = null,
    val collisionThumbnail: UriPath? = null,
    val thumbnail: UriPath? = null,
    var choice: NameCollisionChoice? = null,
)