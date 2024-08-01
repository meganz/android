package mega.privacy.android.domain.entity.node.namecollision

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * NodeNameCollisionResult
 */
@Serializable
data class NodeNameCollisionResult(
    val nameCollision: NameCollision,
    val collisionName: String? = null,
    val collisionSize: Long? = null,
    val collisionFolderContent: FolderTreeInfo? = null,
    val collisionLastModified: Long? = null,
    val collisionThumbnail: UriPath? = null,
    val renameName: String? = null,
    val thumbnail: UriPath? = null,
    var choice: NameCollisionChoice? = null,
)