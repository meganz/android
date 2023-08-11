package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import java.io.File
import javax.inject.Inject


/**
 * UseCase for generating preview for a file
 */
class GeneratePreviewUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * invoke
     * @param handle
     * @param file
     */
    suspend operator fun invoke(handle: Long, file: File) {
        thumbnailPreviewRepository.createPreview(handle, file)
    }
}
