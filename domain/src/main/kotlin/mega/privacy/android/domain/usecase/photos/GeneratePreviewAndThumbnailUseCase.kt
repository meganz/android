package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject


/**
 * UseCase for generating preview and thumbnail for a file
 */
class GeneratePreviewAndThumbnailUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {

    /**
     * invoke
     * @param handle
     * @param file
     */
    suspend operator fun invoke(handle: Long, file: File) {
        imageRepository.generatePreview(handle, file)
        imageRepository.generateThumbnail(handle, file)
    }

}
