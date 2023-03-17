package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get albums
 *
 * @property photosRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetDefaultAlbumPhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetDefaultAlbumPhotos {
    override fun invoke(list: List<suspend (Photo) -> Boolean>) =
        photosRepository.monitorPhotos()
            .mapLatest { filterPhotos(list, it) }

    private suspend fun filterPhotos(
        filters: List<suspend (Photo) -> Boolean>,
        photos: List<Photo>,
    ): List<Photo> = withContext(defaultDispatcher) {
        try {
            photos.filter { photo ->
                filters.any { filter -> filter(photo) }
            }
        } catch (e: Exception) {
            photos
        }
    }
}