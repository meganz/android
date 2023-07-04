package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get public node thumbnail
 * @param imageRepository ImageRepository
 */
class GetPublicNodeThumbnailUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    /**
     * get thumbnail from local if exist, from server otherwise
     * @return File
     */
    suspend operator fun invoke(nodeId: Long): File? {
        runCatching {
            imageRepository.getPublicNodeThumbnailFromLocal(nodeId)
                ?: imageRepository.getPublicNodeThumbnailFromServer(nodeId)
        }.fold(
            onSuccess = { return it },
            onFailure = { return null }
        )
    }
}