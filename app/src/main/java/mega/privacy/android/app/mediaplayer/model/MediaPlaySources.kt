package mega.privacy.android.app.mediaplayer.model

import com.google.android.exoplayer2.MediaItem

/**
 * The media play source item
 *
 * @property mediaItems MediaItem
 * @property newIndexForCurrentItem the new index for playing
 * @property nameToDisplay the name that is displayed
 */
data class MediaPlaySources(
    val mediaItems: List<MediaItem>,
    val newIndexForCurrentItem: Int,
    val nameToDisplay: String?,
)
