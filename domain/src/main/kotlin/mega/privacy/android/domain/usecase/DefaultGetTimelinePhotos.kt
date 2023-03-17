package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Default get timeline photos
 *
 * @property photosRepository
 */
class DefaultGetTimelinePhotos @Inject constructor(
    private val photosRepository: PhotosRepository,
) : GetTimelinePhotos {
    override fun invoke(): Flow<List<Photo>> = photosRepository.monitorPhotos()
}