package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get public node thumbnail
 * @param thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class GetPublicNodeThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {
    /**
     * get thumbnail from local if exist, from server otherwise
     * @return File
     */
    suspend operator fun invoke(nodeId: Long): File? {
        runCatching {
            thumbnailPreviewRepository.getPublicNodeThumbnailFromLocal(nodeId)
                ?: thumbnailPreviewRepository.getPublicNodeThumbnailFromServer(nodeId)
        }.fold(
            onSuccess = { return it },
            onFailure = { return null }
        )
    }
}