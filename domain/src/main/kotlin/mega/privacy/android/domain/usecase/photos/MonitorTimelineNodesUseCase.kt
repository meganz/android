package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to monitor timeline nodes
 */
class MonitorTimelineNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    operator fun invoke() = photosRepository.monitorImageNodes()
}
