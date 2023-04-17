package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import java.io.File
import javax.inject.Inject

/**
 * The use case to get Image Result given an Image file
 */
class GetImageFromFileUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    /**
     * Invoke
     *
     * @param file              Image file
     * @param highPriority      Flag to request image with high priority
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        file: File,
        highPriority: Boolean,
    ): ImageResult {
        return if (file.exists() && file.canRead()) {
            imageRepository.getImageFromFile(file = file, highPriority = highPriority)
        } else {
            throw IllegalArgumentException("Image file doesn't exist")
        }
    }
}