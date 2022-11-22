package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Album

/**
 * Get user albums use case.
 */
fun interface GetUserAlbums {
    /**
     * Get user albums.
     * @return a flow list of user albums.
     */
    operator fun invoke(): Flow<List<Album.UserAlbum>>
}
