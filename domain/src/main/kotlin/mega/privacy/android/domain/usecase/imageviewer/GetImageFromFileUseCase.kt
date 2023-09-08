package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * The use case to get Image Result given an Image file
 */
class GetImageFromFileUseCase @Inject constructor(
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param file              Image file
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        file: File,
    ): ImageResult {
        return if (file.exists() && file.canRead()) {
            val isVideo = isVideoFileUseCase(file.absolutePath)
            val fileName =
                thumbnailPreviewRepository.getThumbnailOrPreviewFileName(file.name + file.length())

            val previewFilePath =
                "${thumbnailPreviewRepository.getPreviewCacheFolderPath() ?: DEFAULT_PATH}${File.separator}${fileName}"

            if (!fileSystemRepository.doesFileExist(previewFilePath)) {
                thumbnailPreviewRepository.createPreview(file.name + file.length(), file)
            }

            ImageResult(
                isVideo = isVideo,
                previewUri = if (fileSystemRepository.doesFileExist(previewFilePath)) "$FILE$previewFilePath" else null,
                fullSizeUri = "$FILE${file.absolutePath}",
                isFullyLoaded = true
            )
        } else {
            throw IllegalArgumentException("Image file doesn't exist")
        }
    }

    companion object {
        /**
         * Default Path
         */
        private const val DEFAULT_PATH = ""

        /**
         * File path Prefix
         */
        private const val FILE = "file://"
    }
}