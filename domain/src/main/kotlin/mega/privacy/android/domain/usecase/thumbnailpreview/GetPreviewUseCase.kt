package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * The use case implementation class to get node preview
 * @param thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class GetPreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke
     *
     * @param nodeId
     */
    suspend operator fun invoke(nodeId: Long) =
        thumbnailPreviewRepository.getPreviewFromLocal(nodeId)
            ?: thumbnailPreviewRepository.getPreviewFromServer(nodeId)
}