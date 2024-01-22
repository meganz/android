package mega.privacy.android.app.presentation.videosection.mapper

import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the VideoPlaylist to UIVideoPlaylist
 */
class UIVideoPlaylistMapper @Inject constructor() {

    /**
     * Convert to VideoPlaylist to UIVideoPlaylist
     */
    operator fun invoke(videoPlaylist: VideoPlaylist) =
        UIVideoPlaylist(
            id = videoPlaylist.id,
            title = videoPlaylist.title,
            cover = videoPlaylist.cover,
            creationTime = videoPlaylist.creationTime,
            modificationTime = videoPlaylist.modificationTime,
            thumbnailList = videoPlaylist.thumbnailList?.map { path ->
                File(path)
            },
            numberOfVideos = videoPlaylist.numberOfVideos,
            totalDuration = TimeUtils.getVideoDuration(videoPlaylist.totalDuration),
        )
}
