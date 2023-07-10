package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.usecase.filenode.IsValidNodeFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * UseCase to get TypedImageNode from ImageNode
 */
class AddImageTypeUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val isValidNodeFileUseCase: IsValidNodeFileUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(node: ImageNode): TypedImageNode {
        return TypedImageNode(
            imageNode = node,
            thumbnailPath = getThumbnailPath(node.base64Id),
            previewPath = getPreviewPath(node.base64Id),
            fullSizePath = getFullImagePath(node),
            fetchThumbnail = getFetchThumbnailFunction(node.downloadThumbnail, node.base64Id),
            fetchPreview = getFetchPreviewFunction(node.downloadPreview, node.base64Id),
            fetchFullImage = getFetchFullImageFunction(node),
        )
    }

    private suspend fun getThumbnailPath(
        nodeName: String,
    ): String? {
        val path =
            "${imageRepository.getThumbnailPath() ?: DEFAULT_PATH}${File.separator}${nodeName}$EXTENSION_JPG"
        if (fileSystemRepository.doesFileExist(path)) {
            return path
        }
        return null
    }


    private suspend fun getPreviewPath(
        nodeName: String,
    ): String? {
        val path =
            "${imageRepository.getPreviewPath() ?: DEFAULT_PATH}${File.separator}${nodeName}$EXTENSION_JPG"
        if (fileSystemRepository.doesFileExist(path)) {
            return path
        }
        return null
    }


    private suspend fun getFullImagePath(node: ImageNode): String? {
        val path =
            "${imageRepository.getFullImagePath() ?: DEFAULT_PATH}${File.separator}${node.base64Id}.${node.type.extension}"
        val fullSizeFile = File(path)
        if (!isValidNodeFileUseCase(node, fullSizeFile)) {
            fileSystemRepository.deleteFile(fullSizeFile)
        }
        if (fileSystemRepository.doesFileExist(path)) {
            return path
        }
        return null
    }

    private suspend fun getFetchThumbnailFunction(
        downloadThumbnail: suspend (String) -> String,
        nodeName: String,
    ): suspend () -> String = {
        val path =
            "${imageRepository.getThumbnailPath() ?: DEFAULT_PATH}${File.separator}${nodeName}$EXTENSION_JPG"
        if (fileSystemRepository.doesFileExist(path)) {
            path
        } else {
            downloadThumbnail(path)
        }
    }

    private suspend fun getFetchPreviewFunction(
        downloadPreview: suspend (String) -> String,
        nodeName: String,
    ): suspend () -> String = {
        val path =
            "${imageRepository.getPreviewPath() ?: DEFAULT_PATH}${File.separator}${nodeName}$EXTENSION_JPG"
        if (fileSystemRepository.doesFileExist(path)) {
            path
        } else {
            downloadPreview(path)
        }
    }

    private fun getFetchFullImageFunction(node: ImageNode): (Boolean, () -> Unit) -> Flow<ImageProgress> =
        { isPriority, resetDownloads ->
            flow {
                val path =
                    "${imageRepository.getFullImagePath() ?: DEFAULT_PATH}${File.separator}${node.base64Id}.${node.type.extension}"
                val fullSizeFile = File(path)
                if (!isValidNodeFileUseCase(node, fullSizeFile)) {
                    fileSystemRepository.deleteFile(fullSizeFile)
                }
                if (fileSystemRepository.doesFileExist(path)) {
                    emit(ImageProgress.Completed(path))
                } else {
                    emitAll(
                        node.downloadFullImage(
                            path,
                            isPriority,
                            resetDownloads
                        )
                    )
                }
            }
        }

    companion object {
        /**
         * JPG Extension
         */
        const val EXTENSION_JPG = ".jpg"
        const val DEFAULT_PATH = ""
    }
}