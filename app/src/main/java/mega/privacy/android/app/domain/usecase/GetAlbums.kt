package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.Album

/**
 * The use case interface to get albums
 */
fun interface GetAlbums {
    /**
     * get albums
     * @return Flow<List<Album>>
     */
    operator fun invoke(): Flow<List<Album>>
}