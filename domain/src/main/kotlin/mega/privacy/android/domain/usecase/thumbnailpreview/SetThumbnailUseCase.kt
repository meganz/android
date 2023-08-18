package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * Set thumbnail use case.
 *
 * @property thumbnailPreviewRepository [ThumbnailPreviewRepository]
 */
class SetThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke.
     *
     * @param nodeHandle MegaNode handle to set the thumbnail
     * @param srcFilePath Source path of the file that will be set as thumbnail
     */
    suspend operator fun invoke(nodeHandle: Long, srcFilePath: String) =
        thumbnailPreviewRepository.setThumbnail(nodeHandle, srcFilePath)
}