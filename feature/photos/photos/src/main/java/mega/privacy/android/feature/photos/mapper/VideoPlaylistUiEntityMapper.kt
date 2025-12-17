package mega.privacy.android.feature.photos.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.SystemVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.shared.resources.R as SharedR
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * The mapper class to convert the VideoPlaylist to VideoPlaylistUIEntity
 */
class VideoPlaylistUiEntityMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val videoUiEntityMapper: VideoUiEntityMapper,
    @ApplicationContext private val context: Context,
) {

    /**
     * Convert to VideoPlaylist to VideoPlaylistUIEntity
     */
    operator fun invoke(videoPlaylist: VideoPlaylist) =
        VideoPlaylistUiEntity(
            id = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.id else NodeId(-1),
            title = when (videoPlaylist) {
                is UserVideoPlaylist -> videoPlaylist.title
                is FavouritesVideoPlaylist -> context.getString(SharedR.string.video_section_title_favourite_playlist)
                else -> ""
            },
            cover = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.cover else null,
            creationTime = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.creationTime else 0,
            modificationTime = if (videoPlaylist is UserVideoPlaylist) videoPlaylist.modificationTime else 0,
            thumbnailList = videoPlaylist.videos?.let {
                if (it.isEmpty()) {
                    null
                } else {
                    getNodeIdListRelatedToThumbnail(it)
                }
            },
            numberOfVideos = videoPlaylist.videos?.size ?: 0,
            totalDuration = getTotalDuration(videoPlaylist.videos),
            videos = videoPlaylist.videos?.map {
                videoUiEntityMapper(it)
            },
            isSystemVideoPlayer = videoPlaylist is SystemVideoPlaylist
        )

    private fun getTotalDuration(videos: List<TypedVideoNode>?): String {
        return if (videos.isNullOrEmpty()) {
            ""
        } else {
            val durationSeconds = videos.sumOf { it.duration.inWholeSeconds }.seconds
            durationInSecondsTextMapper(durationSeconds)
        }
    }


    private fun getNodeIdListRelatedToThumbnail(videos: List<TypedVideoNode>) =
        videos.filter {
            it.hasThumbnail
        }.take(
            if (videos.size > MAX_THUMBNAIL_COUNT) {
                MAX_THUMBNAIL_COUNT
            } else {
                videos.size
            }
        ).map { videoNode ->
            videoNode.id
        }

    companion object {
        private const val MAX_THUMBNAIL_COUNT = 4
    }
}
