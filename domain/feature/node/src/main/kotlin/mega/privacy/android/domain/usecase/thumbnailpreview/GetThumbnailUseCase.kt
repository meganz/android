package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case implementation class to get node thumbnail
 * @param thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class GetThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke
     *
     * @param nodeId [Long]
     * @return [File]
     */
    suspend operator fun invoke(nodeId: Long, allowThrow: Boolean = false): File? {
        runCatching {
            thumbnailPreviewRepository.getThumbnailFromLocal(nodeId)
                ?: thumbnailPreviewRepository.getThumbnailFromServer(nodeId)
        }.fold(
            onSuccess = { return it },
            onFailure = { e ->
                if (allowThrow) throw e
                return null
            }
        )
    }
}