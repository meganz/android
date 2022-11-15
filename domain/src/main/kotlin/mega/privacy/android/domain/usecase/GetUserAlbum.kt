package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId

/**
 * Get user album use case
 */
fun interface GetUserAlbum {
    /**
     * Get user album
     * @return a flow of user album if exists
     */
    operator fun invoke(albumId: AlbumId): Flow<Album.UserAlbum?>
}
