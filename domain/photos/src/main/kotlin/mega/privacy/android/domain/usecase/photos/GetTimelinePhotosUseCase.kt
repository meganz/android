package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * The usecase to get Timeline photos
 */
class GetTimelinePhotosUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Get timeline photos
     *
     * @return Flow<List<Photo>>
     */
    operator fun invoke(): Flow<List<Photo>> = photosRepository.monitorPhotos()
}