package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject

/**
 * UseCase for deleting thumbnail file for a image node
 */
class DeleteThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * invoke
     * @param handle [Long]
     * @return [Boolean] if file doesn't exists it returns true else it returns true or false based
     * on the delete operation
     */
    suspend operator fun invoke(handle: Long) =
        thumbnailPreviewRepository.deleteThumbnail(handle) ?: true
}
