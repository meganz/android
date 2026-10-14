package mega.privacy.android.feature.mediaplayer.videoplayer.data.model

import androidx.media3.common.MediaItem

/**
 * What the player should hold, as the queue builder works it out.
 *
 * The builder emits twice for one launch: first the single item that was tapped, so playback can
 * start while the rest is still being resolved, then the whole queue around it. Both arrive as
 * this. What lets the player extend its queue instead of rebuilding it is that the entry at
 * [currentIndex] carries the media id it is already on — the node handle, which is the same every
 * time it is built. The item itself may be a different instance.
 *
 * @property mediaItems the queue, in play order. Never empty: a sibling search that comes back with
 * nothing leaves the tapped video as the whole queue rather than emptying the player.
 * @property currentIndex where in [mediaItems] the item the user asked for sits.
 * @property startedStreamingServer whether this build started the streaming server, which makes
 * the player responsible for stopping it again.
 */
data class VideoPlayQueue(
    val mediaItems: List<MediaItem>,
    val currentIndex: Int,
    val startedStreamingServer: Boolean = false,
)
