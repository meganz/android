package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity
import javax.inject.Inject
import kotlin.collections.map

/**
 * The mapper class to convert the VideoPlaylist to VideoPlaylistDetailUiEntity
 */
class VideoPlaylistDetailUiEntityMapper @Inject constructor(
    private val videoPlaylistUiEntityMapper: VideoPlaylistUiEntityMapper,
    private val videoUiEntityMapper: VideoUiEntityMapper,
) {

    /**
     * Convert to VideoPlaylist to VideoPlaylistDetailUiEntity
     *
     * @param videoPlaylist The video playlist
     * @return The video playlist detail ui entity
     */
    operator fun invoke(
        videoPlaylist: VideoPlaylist,
        showHiddenItems: Boolean,
        selectedIds: Set<Long>,
    ) =
        VideoPlaylistDetailUiEntity(
            uiEntity = videoPlaylistUiEntityMapper(videoPlaylist),
            videos = videoPlaylist.videos?.filterNonSensitiveItems(showHiddenItems)?.map {
                videoUiEntityMapper(it, emptyList()).copy(
                    isSelected = it.id.longValue in selectedIds
                )
            } ?: emptyList()
        )

    private fun List<TypedVideoNode>.filterNonSensitiveItems(showHiddenItems: Boolean) =
        if (showHiddenItems) {
            this
        } else {
            filterNot { it.isMarkedSensitive || it.isSensitiveInherited }
        }
}