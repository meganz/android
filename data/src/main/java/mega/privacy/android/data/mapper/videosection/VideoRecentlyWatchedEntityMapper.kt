package mega.privacy.android.data.mapper.videosection

import mega.privacy.android.data.database.entity.VideoRecentlyWatchedEntity
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import javax.inject.Inject

internal class VideoRecentlyWatchedEntityMapper @Inject constructor() {

    operator fun invoke(item: VideoRecentlyWatchedItem) =
        VideoRecentlyWatchedEntity(item.videoHandle, item.watchedTimestamp)
}