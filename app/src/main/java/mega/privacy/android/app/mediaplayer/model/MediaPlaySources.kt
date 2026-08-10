package mega.privacy.android.app.mediaplayer.model

import androidx.media3.common.MediaItem

/**
 * The media play source item
 *
 * @property mediaItems MediaItem
 * @property newIndexForCurrentItem the new index for playing
 * @property nameToDisplay the name that is displayed
 * @property isRestartPlaying Whether to restart playing the video after the play sources are updated.
 * @property serverStarted Whether the HTTP streaming server was started as part of building this
 *   play queue. The caller is responsible for stopping the server when playback ends.
 */
data class MediaPlaySources(
    val mediaItems: List<MediaItem>,
    val newIndexForCurrentItem: Int,
    val nameToDisplay: String?,
    val isRestartPlaying: Boolean = true,
    val serverStarted: Boolean = false,
)
