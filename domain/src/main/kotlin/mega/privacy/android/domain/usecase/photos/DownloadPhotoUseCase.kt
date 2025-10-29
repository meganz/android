package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.DownloadPhotoRequest
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.domain.usecase.camerauploads.IsFolderPathExistingUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import javax.inject.Inject

class DownloadPhotoUseCase @Inject constructor(
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val downloadPreviewUseCase: DownloadPreviewUseCase,
    private val downloadPublicNodeThumbnailUseCase: DownloadPublicNodeThumbnailUseCase,
    private val downloadPublicNodePreviewUseCase: DownloadPublicNodePreviewUseCase,
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
) {

    suspend operator fun invoke(request: DownloadPhotoRequest): DownloadPhotoResult {
        val path =
            if (request.isPreview) request.photo.previewFilePath else request.photo.thumbnailFilePath
        if (path == null) return DownloadPhotoResult.EmptyFilePath

        if (isFolderPathExistingUseCase(path = path)) {
            return DownloadPhotoResult.Success(
                previewFilePath = request.photo.previewFilePath,
                thumbnailFilePath = request.photo.thumbnailFilePath
            )
        }

        // download file
        val isSuccess = if (request.isPublicNode) {
            downloadPhotoCover(request)
        } else {
            downloadPublicNodePhotoCover(request)
        }
        return if (isSuccess) {
            DownloadPhotoResult.Success(
                previewFilePath = request.photo.previewFilePath,
                thumbnailFilePath = request.photo.thumbnailFilePath
            )
        } else DownloadPhotoResult.Error
    }

    private suspend fun downloadPhotoCover(request: DownloadPhotoRequest): Boolean {
        return if (request.isPreview) {
            downloadPublicNodePreviewUseCase(nodeId = request.photo.id)
        } else {
            downloadPublicNodeThumbnailUseCase(nodeId = request.photo.id)
        }
    }

    private suspend fun downloadPublicNodePhotoCover(request: DownloadPhotoRequest): Boolean {
        return if (request.isPreview) {
            runCatching { downloadPreviewUseCase(nodeId = request.photo.id) }.isSuccess
        } else {
            runCatching { downloadThumbnailUseCase(nodeId = request.photo.id) }.isSuccess
        }
    }
}
