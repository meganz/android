package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class MonitorPaginatedTimelinePhotosUseCase @Inject constructor(
    private val photosRepository: PhotosRepository
) {
    operator fun invoke() = photosRepository.monitorPaginatedPhotos()
}