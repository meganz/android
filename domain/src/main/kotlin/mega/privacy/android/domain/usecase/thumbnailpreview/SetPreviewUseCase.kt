package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * Set preview use case
 *
 * @property thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class SetPreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke.
     *
     * @param nodeHandle MegaNode handle to set the preview
     * @param srcFilePath Source path of the file that will be set as preview
     */
    suspend operator fun invoke(nodeHandle: Long, srcFilePath: String) =
        thumbnailPreviewRepository.setPreview(nodeHandle, srcFilePath)
}