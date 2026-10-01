package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import javax.inject.Inject


/**
 * UseCase thumbnail for a image or video file.
 */
class CreateImageOrVideoThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * Invoke.
     *
     * @param nodeHandle Node handle of the file already in the Cloud.
     * @param uriPath Local uriPath.
     */
    suspend operator fun invoke(nodeHandle: Long, uriPath: UriPath) {
        thumbnailPreviewRepository.createThumbnail(nodeHandle, uriPath)
    }
}
