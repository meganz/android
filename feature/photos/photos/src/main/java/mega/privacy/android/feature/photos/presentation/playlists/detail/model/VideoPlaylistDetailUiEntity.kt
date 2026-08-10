package mega.privacy.android.feature.photos.presentation.playlists.detail.model

import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity

/**
 * The entity for the video playlist detail in videos section
 *
 * @property uiEntity the playlist ui entity
 * @property videos the list of videos in the playlist
 */
data class VideoPlaylistDetailUiEntity(
    val uiEntity: VideoPlaylistUiEntity,
    val videos: List<VideoUiEntity> = emptyList()
)
