package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Album photo element id value class
 *
 * @property id
 * @property nodeId
 * @property albumId
 */
data class AlbumPhotoId(
    val id: Long,
    val nodeId: NodeId,
    val albumId: AlbumId,
) {
    companion object {
        val default: AlbumPhotoId
            get() = AlbumPhotoId(
                id = -1,
                nodeId = NodeId(-1),
                albumId = AlbumId(-1),
            )
    }
}