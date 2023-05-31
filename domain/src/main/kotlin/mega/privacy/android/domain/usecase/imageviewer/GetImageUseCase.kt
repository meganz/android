package mega.privacy.android.domain.usecase.imageviewer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.TypedImageNode
import javax.inject.Inject

/**
 * Fetch Thumbnail, Preview and Full Size Image given ImageNode
 */
class GetImageUseCase @Inject constructor(
    private val isFullSizeRequiredUseCase: IsFullSizeRequiredUseCase,
) {
    /**
     * Invoke
     *
     * @param node                  Typed Image Node
     * @param fullSize              Flag to request full size image despite data/size requirements
     * @param highPriority          Flag to request image with high priority
     * @param resetDownloads        Callback to reset downloads
     *
     * @return Flow<ImageResult>
     */
    operator fun invoke(
        node: TypedImageNode,
        fullSize: Boolean,
        highPriority: Boolean,
        resetDownloads: () -> Unit,
    ): Flow<ImageResult> = flow {
        val imageResult = ImageResult(isVideo = node.type is VideoFileTypeInfo)
        emit(imageResult)

        runCatching {
            node.fetchThumbnail()
        }.onSuccess {
            imageResult.thumbnailUri = "$FILE$it"
            emit(imageResult)
        }

        val fullSizeRequired = isFullSizeRequiredUseCase(
            node,
            fullSize
        )

        runCatching {
            node.fetchPreview()
        }.onSuccess {
            imageResult.previewUri = "$FILE$it"
            if (fullSizeRequired) {
                emit(imageResult)
            } else {
                imageResult.isFullyLoaded = true
                emit(imageResult)
                return@flow
            }
        }.onFailure { exception ->
            if (!fullSizeRequired) {
                throw exception
            }
        }

        if (fullSizeRequired) {
            node.fetchFullImage(highPriority) {
                resetDownloads()
            }.catch { exception -> throw exception }.collect { result ->
                when (result) {
                    is ImageProgress.Started -> {
                        imageResult.transferTag = result.transferTag
                        emit(imageResult)
                    }

                    is ImageProgress.InProgress -> {
                        imageResult.totalBytes = result.totalBytes
                        imageResult.transferredBytes = result.transferredBytes
                        emit(imageResult)
                    }

                    is ImageProgress.Completed -> {
                        imageResult.isFullyLoaded = true
                        imageResult.fullSizeUri = "$FILE${result.path}"
                        emit(imageResult)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * File path Prefix
         */
        const val FILE = "file://"
    }
}

