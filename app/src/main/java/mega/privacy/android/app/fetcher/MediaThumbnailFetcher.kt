package mega.privacy.android.app.fetcher

import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.domain.usecase.camerauploads.IsFolderPathExistingUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Inject

class MediaThumbnailFetcher(
    private val options: Options,
    private val request: MediaThumbnailRequest,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val downloadPreviewUseCase: DownloadPreviewUseCase,
    private val downloadPublicNodeThumbnailUseCase: DownloadPublicNodeThumbnailUseCase,
    private val downloadPublicNodePreviewUseCase: DownloadPublicNodePreviewUseCase,
    private val downloadPublicAlbumPhotoUseCase: DownloadPublicAlbumPhotoUseCase,
    private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
    private val fileTypeIconMapper: FileTypeIconMapper,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val path = if (request.isPreview) {
            request.previewFilePath
        } else request.thumbnailFilePath

        if (path == null) {
            return getPlaceholderResult()
        }

        if (isFolderPathExistingUseCase(path = path)) {
            return SourceFetchResult(
                source = ImageSource(file = path.toPath(), fileSystem = FileSystem.SYSTEM),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        }

        // download file
        val isSuccess = when {
            request.isPublicAlbumPhoto -> {
                downloadPublicAlbumPhotoUseCase(
                    photoId = request.id,
                    path = path,
                    isPreview = request.isPreview
                )
            }

            request.isPublicNode -> {
                downloadPublicNodePhotoCover(nodeId = request.id)
            }

            else -> downloadPhotoCover(nodeId = request.id)
        }

        return if (isSuccess) {
            SourceFetchResult(
                source = ImageSource(file = path.toPath(), fileSystem = FileSystem.SYSTEM),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        } else getPlaceholderResult()
    }

    private fun getPlaceholderResult(): FetchResult {
        val defaultIconRes = fileTypeIconMapper(request.fileExtension)
        val drawable = ContextCompat.getDrawable(options.context, defaultIconRes)
            ?: throw IllegalArgumentException("Icon with res id: $defaultIconRes not found!")
        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    private suspend fun downloadPublicNodePhotoCover(nodeId: Long) = if (request.isPreview) {
        runCatching { downloadPublicNodePreviewUseCase(nodeId = nodeId) }.getOrDefault(false)
    } else {
        runCatching { downloadPublicNodeThumbnailUseCase(nodeId = nodeId) }.getOrDefault(false)
    }

    private suspend fun downloadPhotoCover(nodeId: Long): Boolean {
        return if (request.isPreview) {
            runCatching { downloadPreviewUseCase(nodeId = nodeId) }.isSuccess
        } else {
            runCatching { downloadThumbnailUseCase(nodeId = nodeId) }.isSuccess
        }
    }

    class Factory @Inject constructor(
        private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
        private val downloadPreviewUseCase: DownloadPreviewUseCase,
        private val downloadPublicNodeThumbnailUseCase: DownloadPublicNodeThumbnailUseCase,
        private val downloadPublicNodePreviewUseCase: DownloadPublicNodePreviewUseCase,
        private val downloadPublicAlbumPhotoUseCase: DownloadPublicAlbumPhotoUseCase,
        private val isFolderPathExistingUseCase: IsFolderPathExistingUseCase,
        private val fileTypeIconMapper: FileTypeIconMapper,
    ) : Fetcher.Factory<MediaThumbnailRequest> {

        override fun create(
            data: MediaThumbnailRequest,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = MediaThumbnailFetcher(
            request = data,
            options = options,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
            downloadPreviewUseCase = downloadPreviewUseCase,
            downloadPublicNodeThumbnailUseCase = downloadPublicNodeThumbnailUseCase,
            downloadPublicNodePreviewUseCase = downloadPublicNodePreviewUseCase,
            downloadPublicAlbumPhotoUseCase = downloadPublicAlbumPhotoUseCase,
            isFolderPathExistingUseCase = isFolderPathExistingUseCase,
            fileTypeIconMapper = fileTypeIconMapper
        )
    }
}
