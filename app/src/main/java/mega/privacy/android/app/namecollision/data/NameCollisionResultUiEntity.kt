package mega.privacy.android.app.namecollision.data

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult

/**
 * Data class containing all the required to present a name collision for an upload, copy or movement.
 * It's also used when data needs to be transferred between activities via intent
 *
 * @property nameCollision          [NameCollisionUiEntity]
 * @property collisionName          The node name with which there is a name collision.
 * @property collisionSize          The node size with which there is a name collision if is a file, null otherwise.
 * @property collisionFolderContent The folder content of the node with which there is a name collision if is a folder, null otherwise.
 * @property collisionLastModified  The node last modified date with which there is a name collision.
 * @property collisionThumbnail     The node thumbnail if exists.
 * @property renameName             Name of the item for the rename option. Null if if the item is a folder.
 * @property thumbnail              The thumbnail of the item to upload, copy or move if exists.
 * @property choice                 [NameCollisionChoice] with the collision resolution.
 */
@Parcelize
data class NameCollisionResultUiEntity(
    val nameCollision: NameCollisionUiEntity,
    var collisionName: String? = null,
    var collisionSize: Long? = null,
    var collisionFolderContent: String? = null,
    var collisionLastModified: Long? = null,
    var collisionThumbnail: Uri? = null,
    var renameName: String? = null,
    var thumbnail: Uri? = null,
    var choice: NameCollisionChoice? = null,
) : Parcelable


/**
 * Convert domain module's [NodeNameCollisionResult] to app module's [NameCollisionResultUiEntity]
 */
fun NodeNameCollisionResult.toUiEntity() = NameCollisionResultUiEntity(
    nameCollision = nameCollision.toUiEntity(),
    collisionName = collisionName,
    collisionSize = collisionSize,
    collisionFolderContent = collisionFolderContent?.toString(),
    collisionLastModified = collisionLastModified,
    collisionThumbnail = runCatching { collisionThumbnail?.value?.toUri() }.getOrNull(),
    renameName = renameName,
    thumbnail = runCatching { thumbnail?.value?.toUri() }.getOrNull(),
    choice = choice,
)