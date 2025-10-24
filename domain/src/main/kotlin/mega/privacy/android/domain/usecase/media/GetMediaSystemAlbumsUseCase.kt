package mega.privacy.android.domain.usecase.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case for retrieving predefined system albums from the photo repository.
 *
 * This use case builds a list of [MediaAlbum.System] objects representing system-defined media
 * categories. Each album includes a cover photo (if available) and uses the configured
 * [SystemAlbum] to determine which media items belong to that album.
 *
 * The album covers are determined by the first matching photo for each filter, if any exist.
 *
 *  @property photosRepository Repository providing access to photo data.
 *  @property systemAlbums Set of configured system album types.
 *  @property defaultDispatcher Coroutine dispatcher used for background execution.
 */
class GetMediaSystemAlbumsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val systemAlbums: Set<@JvmSuppressWildcards SystemAlbum>,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {
    /**
     * Retrieves the list of system-defined media albums.
     *
     * @return List of [MediaAlbum.System] each representing a predefined system album.
     */
    suspend operator fun invoke(): List<MediaAlbum> = withContext(defaultDispatcher) {
        val photos = photosRepository.monitorPhotos()

        systemAlbums.map { albumType ->
            val cover = photos.first().firstOrNull { photo ->
                albumType.filter(photo)
            }
            MediaAlbum.System(id = albumType, cover = cover)
        }
    }
}