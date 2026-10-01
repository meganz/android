package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * The use case interface to download thumbnail
 */
class DownloadThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {
    /**
     * Download thumbnail
     */
    suspend operator fun invoke(nodeId: Long) {
        thumbnailPreviewRepository.downloadThumbnail(handle = nodeId)
    }
}