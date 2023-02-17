package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.ImageRepository
import java.io.File

/**
 * Default Implementation of [GetImageFromFile]
 */
class DefaultGetImageFromFile(
    private val imageRepository: ImageRepository,
) : GetImageFromFile {
    override suspend fun invoke(
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