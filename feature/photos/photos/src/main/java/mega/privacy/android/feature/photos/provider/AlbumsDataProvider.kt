package mega.privacy.android.feature.photos.provider

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.media.MediaAlbum

/**
 * Interface for providing album data streams in the photos feature.
 *
 * Providers supply different types of media albums (system albums, user albums, etc.)
 * and are injected as a Set to combine their data. Albums are sorted by priority.
 *
 */
interface AlbumsDataProvider {
    /**
     * Priority for album ordering. Lower values = higher priority.
     */
    val order: Int

    /**
     * Monitors and provides a stream of media albums.
     *
     * @return A Flow that emits the complete list of albums whenever there are updates
     */
    fun monitorAlbums(): Flow<List<MediaAlbum>>
}