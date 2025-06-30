package mega.privacy.android.app.fetcher

import coil3.key.Keyer
import coil3.request.Options
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest

/**
 * Mega thumbnail keyer to build the key for MegaNode thumbnail in the memory cache
 *
 */
internal object MegaThumbnailKeyer : Keyer<ThumbnailData> {
    override fun key(data: ThumbnailData, options: Options): String = when (data) {
        is ThumbnailRequest -> "${data.id.longValue}-${options.size}"
        is ChatThumbnailRequest -> "${data.chatId}-${data.messageId}-${options.size}"
    }
}