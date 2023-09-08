package mega.privacy.android.domain.usecase.imageviewer

import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * The use case to get Image Result given Offline Node Handle
 */
class GetImageByOfflineNodeHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val isVideoFileUseCase: IsVideoFileUseCase,
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     *
     * @param nodeHandle                Image Offline File node handle
     *
     * @return ImageResult
     */
    suspend operator fun invoke(
        nodeHandle: Long,
    ): ImageResult {
        nodeRepository.getOfflineNodeInformation(nodeHandle)?.let { nodeInformation ->
            getOfflineFileUseCase(nodeInformation).let { file ->
                if (!file.exists()) throw IllegalArgumentException("Offline file doesn't exist")
                val isVideo = isVideoFileUseCase(file.absolutePath)

                try {
                    val offlineNodeHandle = nodeInformation.handle.toLong()
                    val fileName =
                        thumbnailPreviewRepository.getThumbnailOrPreviewFileName(offlineNodeHandle)

                    val thumbnailFilePath =
                        "${thumbnailPreviewRepository.getThumbnailCacheFolderPath() ?: DEFAULT_PATH}${File.separator}${fileName}"

                    val previewFilePath =
                        "${thumbnailPreviewRepository.getPreviewCacheFolderPath() ?: DEFAULT_PATH}${File.separator}${fileName}"

                    if (!fileSystemRepository.doesFileExist(previewFilePath)) {
                        thumbnailPreviewRepository.createPreview(
                            offlineNodeHandle,
                            file
                        )
                    }
                    return ImageResult(
                        isVideo = isVideo,
                        thumbnailUri = if (fileSystemRepository.doesFileExist(thumbnailFilePath)) "$FILE$thumbnailFilePath" else null,
                        previewUri = if (fileSystemRepository.doesFileExist(previewFilePath)) "$FILE$previewFilePath" else null,
                        fullSizeUri = "$FILE${file.absolutePath}",
                        isFullyLoaded = true
                    )
                } catch (e: NumberFormatException) {
                    return ImageResult(
                        isVideo = isVideo,
                        fullSizeUri = "$FILE${file.absolutePath}",
                        isFullyLoaded = true
                    )
                }
            }
        } ?: throw IllegalArgumentException("Offline node was not found")
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