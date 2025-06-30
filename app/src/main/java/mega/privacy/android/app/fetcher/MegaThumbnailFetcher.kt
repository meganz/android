package mega.privacy.android.app.fetcher

import android.webkit.MimeTypeMap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.thumbnailpreview.GetChatThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/**
 * Mega thumbnail fetcher to load thumbnails from normal MegaNode
 */
internal class MegaThumbnailFetcher(
    private val request: ThumbnailData,
    private val getThumbnailUseCase: dagger.Lazy<GetThumbnailUseCase>,
    private val getPublicNodeThumbnailUseCase: dagger.Lazy<GetPublicNodeThumbnailUseCase>,
    private val getChatThumbnailUseCase: dagger.Lazy<GetChatThumbnailUseCase>,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val file = when (request) {
            is ChatThumbnailRequest -> getChatThumbnailUseCase.get()(
                chatId = request.chatId,
                messageId = request.messageId
            )

            is ThumbnailRequest -> if (request.isPublicNode) {
                getPublicNodeThumbnailUseCase.get()(request.id.longValue, true)
            } else {
                getThumbnailUseCase.get()(request.id.longValue, true)
            }
        } ?: throw NullPointerException("Thumbnail file is null")
        return SourceFetchResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            dataSource = DataSource.DISK
        )
    }

    /**
     * Factory
     */
    class Factory @Inject constructor(
        private val getThumbnailUseCase: dagger.Lazy<GetThumbnailUseCase>,
        private val getPublicNodeThumbnailUseCase: dagger.Lazy<GetPublicNodeThumbnailUseCase>,
        private val getChatThumbnailUseCase: dagger.Lazy<GetChatThumbnailUseCase>,
    ) : Fetcher.Factory<ThumbnailData> {

        override fun create(
            data: ThumbnailData,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return MegaThumbnailFetcher(
                request = data,
                getThumbnailUseCase = getThumbnailUseCase,
                getPublicNodeThumbnailUseCase = getPublicNodeThumbnailUseCase,
                getChatThumbnailUseCase = getChatThumbnailUseCase
            )
        }

        private fun isApplicable(data: ThumbnailData): Boolean {
            return (data is ThumbnailRequest && data.id.longValue != -1L)
                    || (data is ChatThumbnailRequest && data.chatId != -1L && data.messageId != -1L)
        }
    }
}