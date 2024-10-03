package mega.privacy.android.data.mapper.videosection

import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import javax.inject.Inject

/**
 * Mapper for converting to VideoRecentlyWatchedItem
 */
class VideoRecentlyWatchedItemMapper @Inject constructor() {

    /**
     * Converts to VideoRecentlyWatchedItem
     */
    operator fun invoke(
        videoHandle: Long,
        watchedTimestamp: Long,
        collectionId: Long = 0,
        collectionTitle: String? = null,
    ) = VideoRecentlyWatchedItem(
        videoHandle = videoHandle,
        watchedTimestamp = watchedTimestamp,
        collectionId = collectionId,
        collectionTitle = collectionTitle
    )
}