package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject


/**
 * UseCase thumbnail for a file
 */
class GenerateThumbnailUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {

    /**
     * invoke
     * @param handle
     * @param file
     */
    suspend operator fun invoke(handle: Long, file: File) {
        imageRepository.createThumbnail(handle, file)
    }
}
