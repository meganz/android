package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * The use case interface to download preview
 */
class DownloadPreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {
    /**
     * Download preview
     */
    suspend operator fun invoke(nodeId: Long) {
        thumbnailPreviewRepository.downloadPreview(nodeId)
    }
}
