package mega.privacy.android.app.fetcher.legacy

import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import dagger.Lazy
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.thumbnailpreview.GetChatThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPublicNodeThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/**
 * Mega thumbnail fetcher to load thumbnails from normal MegaNode
 */
internal class LegacyMegaThumbnailFetcher(
    private val request: ThumbnailData,
    private val getThumbnailUseCase: Lazy<GetThumbnailUseCase>,
    private val getPublicNodeThumbnailUseCase: Lazy<GetPublicNodeThumbnailUseCase>,
    private val getChatThumbnailUseCase: Lazy<GetChatThumbnailUseCase>,
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
        return SourceResult(
            source = ImageSource(file = file.toOkioPath()),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension),
            dataSource = DataSource.DISK
        )
    }

    /**
     * Factory
     */
    class Factory @Inject constructor(
        private val getThumbnailUseCase: Lazy<GetThumbnailUseCase>,
        private val getPublicNodeThumbnailUseCase: Lazy<GetPublicNodeThumbnailUseCase>,
        private val getChatThumbnailUseCase: Lazy<GetChatThumbnailUseCase>,
    ) : Fetcher.Factory<ThumbnailData> {

        override fun create(
            data: ThumbnailData,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return LegacyMegaThumbnailFetcher(
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