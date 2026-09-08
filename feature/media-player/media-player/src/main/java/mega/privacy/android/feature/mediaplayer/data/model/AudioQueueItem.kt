package mega.privacy.android.feature.mediaplayer.data.model

/**
 * A single item in the audio play queue, derived from Media3 [androidx.media3.common.MediaItem] metadata.
 *
 * @property mediaId Opaque UUID assigned to the [androidx.media3.common.MediaItem]; used to identify the item within the queue.
 * @property title Title from [androidx.media3.common.MediaMetadata]; `null` if the item has no title metadata.
 * @property artist Artist from [androidx.media3.common.MediaMetadata]; `null` if the item has no artist metadata.
 * @property handle MEGA node handle resolved via [mega.privacy.android.feature.mediaplayer.data.MediaHandleStore]; `null` if the item is not backed by a MEGA node.
 * @property playerIndex The item's fixed index in the media player, as passed to the seek APIs.
 * Under shuffle the queue is displayed in playback order, which differs from this index order,
 * so seeking must use [playerIndex] rather than the item's position in the displayed list.
 */
data class AudioQueueItem(
    val mediaId: String,
    val title: String?,
    val artist: String?,
    val handle: Long? = null,
    val playerIndex: Int = 0,
)
