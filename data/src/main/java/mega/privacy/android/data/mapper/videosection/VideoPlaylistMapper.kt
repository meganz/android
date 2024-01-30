package mega.privacy.android.data.mapper.videosection

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class VideoPlaylistMapper @Inject constructor() {

    operator fun invoke(
        userSet: UserSet,
        videoNodeList: List<TypedVideoNode>,
    ) = VideoPlaylist(
        id = NodeId(userSet.id),
        title = userSet.name,
        cover = userSet.cover,
        creationTime = userSet.creationTime,
        modificationTime = userSet.modificationTime,
        thumbnailList = if (videoNodeList.isEmpty()) {
            null
        } else {
            getThumbnailList(videoNodeList)
        },
        numberOfVideos = videoNodeList.size,
        totalDuration = videoNodeList.sumOf { it.duration.inWholeSeconds }.seconds
    )

    private fun getThumbnailList(videoNodeList: List<TypedVideoNode>) =
        videoNodeList.take(
            if (videoNodeList.size > MAX_THUMBNAIL_COUNT) {
                MAX_THUMBNAIL_COUNT
            } else {
                videoNodeList.size
            }
        ).map { videoNode ->
            videoNode.thumbnailPath
        }

    companion object {
        private const val MAX_THUMBNAIL_COUNT = 4
    }
}