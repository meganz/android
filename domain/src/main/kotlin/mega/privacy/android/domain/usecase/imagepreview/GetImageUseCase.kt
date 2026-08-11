package mega.privacy.android.domain.usecase.imagepreview

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaEventUseCase
import javax.inject.Inject

/**
 * Fetch Thumbnail, Preview and Full Size Image given ImageNode
 */
class GetImageUseCase @Inject constructor(
    private val isFullSizeRequiredUseCase: IsFullSizeRequiredUseCase,
    private val photosRepository: PhotosRepository,
    private val broadcastTransferOverQuotaEventUseCase: BroadcastTransferOverQuotaEventUseCase,
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
    ): Flow<ImageResult> {
        return photosRepository.monitorImageResult(node.id) ?: flow {
            val imageResult = ImageResult(
                isVideo = node.type is VideoFileTypeInfo,
                thumbnailUri = node.thumbnailPath?.let { "$FILE$it" },
                previewUri = node.previewPath?.let { "$FILE$it" },
                fullSizeUri = node.fullSizePath?.let { "$FILE$it" },
            )

            val fullSizeRequired = isFullSizeRequiredUseCase(node, fullSize)

            if ((!fullSizeRequired && node.previewPath != null) || node.fullSizePath != null) {
                imageResult.isFullyLoaded = true
                emit(imageResult)
                photosRepository.saveImageResult(node.id, imageResult)
                return@flow
            } else {
                emit(imageResult)
                photosRepository.saveImageResult(node.id, imageResult)
            }

            if (node.thumbnailPath == null) {
                runCatching {
                    node.fetchThumbnail()
                }.onSuccess {
                    imageResult.thumbnailUri = "$FILE$it"
                    emit(imageResult)
                    photosRepository.saveImageResult(node.id, imageResult)
                }
            }

            if (node.previewPath == null) {
                runCatching {
                    node.fetchPreview()
                }.onSuccess {
                    imageResult.previewUri = "$FILE$it"
                    if (fullSizeRequired) {
                        emit(imageResult)
                        photosRepository.saveImageResult(node.id, imageResult)
                    } else {
                        imageResult.isFullyLoaded = true
                        emit(imageResult)
                        photosRepository.saveImageResult(node.id, imageResult)
                        return@flow
                    }
                }.onFailure { exception ->
                    if (!fullSizeRequired) {
                        throw exception
                    }
                }
            }

            if (fullSizeRequired) {
                node.fetchFullImage(highPriority) {
                    resetDownloads()
                }.catch { exception ->
                    if (exception.isTransferOverQuota()) {
                        broadcastTransferOverQuota()
                    }
                    throw exception
                }.collect { result ->
                    when (result) {
                        is ImageProgress.Started -> {
                            imageResult.transferTag = result.transferTag
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }

                        is ImageProgress.InProgress -> {
                            imageResult.totalBytes = result.totalBytes
                            imageResult.transferredBytes = result.transferredBytes
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }

                        is ImageProgress.Completed -> {
                            imageResult.isFullyLoaded = true
                            imageResult.fullSizeUri = "$FILE${result.path}"
                            emit(imageResult)
                            photosRepository.saveImageResult(node.id, imageResult)
                        }
                    }
                }
            }
        }.onCompletion { cause ->
            // If this download flow terminates (normally, on error, or — most importantly —
            // because the collector was cancelled, e.g. the user swiped to another page) before
            // fully loading, evict the partial cache entry so the node is re-fetched on the next
            // access instead of being stuck on a low-res preview or a black frame. No-op when the
            // download completed (saveImageResult already removed the entry).
            //
            // Bandwidth over quota is the exception: the download cannot progress until the quota
            // window ends, so keeping the entry is what stops the viewer from restarting it, and
            // raising the warning again, every time the screen recomposes — including when it
            // recomposes on returning from that very warning.
            if (cause?.isTransferOverQuota() != true) {
                photosRepository.clearImageResult(node.id)
            }
        }
    }

    /**
     * The download is cancelled with the cause wrapped in a [kotlinx.coroutines.CancellationException],
     * so the quota failure is only reachable through it.
     */
    private fun Throwable.isTransferOverQuota(): Boolean =
        this is QuotaExceededMegaException || cause is QuotaExceededMegaException

    /**
     * Full size images are downloaded on their own SDK listener rather than through the monitored
     * transfer pipeline, so nothing else reports the bandwidth over quota they hit. Without this
     * the failure never reaches the user, who is left looking at a stale thumbnail.
     *
     * Failing to broadcast must not replace the image error being rethrown to the caller.
     */
    private suspend fun broadcastTransferOverQuota() {
        runCatching {
            broadcastTransferOverQuotaEventUseCase(TransferOverQuotaStatus.OverQuota)
        }
    }

    companion object {
        /**
         * File path Prefix
         */
        const val FILE = "file://"
    }
}

