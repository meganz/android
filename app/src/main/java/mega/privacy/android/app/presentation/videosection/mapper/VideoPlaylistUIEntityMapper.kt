package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import javax.inject.Inject

/**
 * The mapper class to convert the VideoPlaylist to VideoPlaylistUIEntity
 */
class VideoPlaylistUIEntityMapper @Inject constructor(
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val videoUIEntityMapper: VideoUIEntityMapper
) {

    /**
     * Convert to VideoPlaylist to VideoPlaylistUIEntity
     */
    operator fun invoke(videoPlaylist: VideoPlaylist) =
        VideoPlaylistUIEntity(
            id = videoPlaylist.id,
            title = videoPlaylist.title,
            cover = videoPlaylist.cover,
            creationTime = videoPlaylist.creationTime,
            modificationTime = videoPlaylist.modificationTime,
            thumbnailList = videoPlaylist.thumbnailList,
            numberOfVideos = videoPlaylist.numberOfVideos,
            totalDuration = durationInSecondsTextMapper(videoPlaylist.totalDuration),
            videos = videoPlaylist.videos?.map {
                videoUIEntityMapper(it)
            }
        )
}
