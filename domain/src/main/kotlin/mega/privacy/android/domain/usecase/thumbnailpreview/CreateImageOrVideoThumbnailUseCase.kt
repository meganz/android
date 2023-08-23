package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import java.io.File
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
     * @param localFile Local file.
     */
    suspend operator fun invoke(nodeHandle: Long, localFile: File) {
        thumbnailPreviewRepository.createThumbnail(nodeHandle, localFile)
    }
}
