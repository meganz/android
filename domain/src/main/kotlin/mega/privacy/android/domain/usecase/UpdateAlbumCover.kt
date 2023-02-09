package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Update album cover use case
 */
fun interface UpdateAlbumCover {
    suspend operator fun invoke(albumId: AlbumId, elementId: NodeId)
}
