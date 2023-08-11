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
     * @param callback success true, fail false
     */
    suspend operator fun invoke(nodeId: Long, callback: (success: Boolean) -> Unit) =
        thumbnailPreviewRepository.downloadThumbnail(nodeId, callback)
}