package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get images nodes use case
 */
class MonitorPublicImageNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Invoke use case
     */
    operator fun invoke(url: String): Flow<List<ImageNode>> = flow {
        emit(listOfNotNull(photosRepository.fetchImageNode(url)))
    }
}
