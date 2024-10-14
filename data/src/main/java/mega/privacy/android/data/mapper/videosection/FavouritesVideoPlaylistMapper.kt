package mega.privacy.android.data.mapper.videosection

import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class FavouritesVideoPlaylistMapper @Inject constructor() {

    operator fun invoke(
        videoNodeList: List<TypedVideoNode>,
    ) = FavouritesVideoPlaylist(
        thumbnailList = if (videoNodeList.isEmpty()) {
            null
        } else {
            getNodeIdListRelatedToThumbnail(videoNodeList)
        },
        numberOfVideos = videoNodeList.size,
        totalDuration = videoNodeList.sumOf { it.duration.inWholeSeconds }.seconds,
        videos = videoNodeList
    )

    private fun getNodeIdListRelatedToThumbnail(videoNodeList: List<TypedVideoNode>) =
        videoNodeList.filter {
            it.hasThumbnail
        }.take(
            if (videoNodeList.size > MAX_THUMBNAIL_COUNT) {
                MAX_THUMBNAIL_COUNT
            } else {
                videoNodeList.size
            }
        ).map { videoNode ->
            videoNode.id
        }

    companion object {
        private const val MAX_THUMBNAIL_COUNT = 4
    }
}