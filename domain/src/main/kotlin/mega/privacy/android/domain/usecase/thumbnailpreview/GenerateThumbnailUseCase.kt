package mega.privacy.android.domain.usecase.thumbnailpreview

import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import java.io.File
import javax.inject.Inject


/**
 * UseCase thumbnail for a file
 */
class GenerateThumbnailUseCase @Inject constructor(
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
) {

    /**
     * invoke
     * @param handle
     * @param file
     */
    suspend operator fun invoke(handle: Long, file: File) {
        thumbnailPreviewRepository.createThumbnail(handle, file)
    }
}
