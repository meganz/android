package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Add photos to album use case
 */
fun interface AddPhotosToAlbum {
    /**
     * Add photos to album
     *
     * @param albumId is the destination album
     * @param photoIds is the list of photo ids to be added in album
     */
    suspend operator fun invoke(albumId: AlbumId, photoIds: List<NodeId>)
}
