package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject


/**
 * UseCase for generating preview for a image or video file.
 */
class CreateImageOrVideoPreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param uriPath Local file.
     */
    suspend operator fun invoke(nodeHandle: Long, uriPath: UriPath) {
        thumbnailPreviewRepository.createPreview(nodeHandle, uriPath)
    }
}
