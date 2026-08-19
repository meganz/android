package mega.privacy.android.feature.mediaplayer.data

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import javax.inject.Inject
import mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem

/**
 * Builds [AudioQueueItem]s from the player's [MediaItem]s, resolving each item's title and
 * remembering tag-derived (e.g. ID3) metadata across item transitions.
 *
 * Title resolution order: tag-derived title when available, otherwise the file name stored as
 * [MediaMetadata.displayTitle] by AudioNodeToMediaItemMapper. Media3 exposes extracted tags only
 * through the combined metadata of the **current** item and never writes them back into the
 * playlist's [MediaItem]s, so without the cache kept here a queue item would fall back to its
 * file name again as soon as playback moves to the next track.
 *
 * All access is expected on the main thread (driven by Player.Listener callbacks).
 */
class AudioQueueItemFactory @Inject constructor(
    private val mediaHandleStore: MediaHandleStore,
) {

    private val parsedTagMetadata = mutableMapOf<String, ParsedTagMetadata>()

    private data class ParsedTagMetadata(val title: String, val artist: String?)

    /** Tag-derived title when available, otherwise the file name stored as displayTitle. */
    fun resolveTitle(metadata: MediaMetadata): String? =
        metadata.title?.toString() ?: metadata.displayTitle?.toString()

    /**
     * Remembers the tag-derived title/artist for [mediaId] so later [buildQueueItems] calls keep
     * showing them once playback moves on.
     *
     * @return true when tag metadata was cached, false when [metadata] carries no tag title
     */
    fun cacheParsedMetadata(mediaId: String, metadata: MediaMetadata): Boolean {
        val tagTitle = metadata.title?.toString() ?: return false
        parsedTagMetadata[mediaId] = ParsedTagMetadata(tagTitle, metadata.artist?.toString())
        return true
    }

    fun buildQueueItems(items: List<MediaItem>): List<AudioQueueItem> =
        items.map { item ->
            val parsed = parsedTagMetadata[item.mediaId]
            AudioQueueItem(
                mediaId = item.mediaId,
                title = parsed?.title ?: resolveTitle(item.mediaMetadata),
                artist = parsed?.artist ?: item.mediaMetadata.artist?.toString(),
                handle = mediaHandleStore.getHandle(item.mediaId),
            )
        }

    /** Drops cached metadata for items no longer in the queue. */
    fun pruneCache(activeMediaIds: Set<String>) {
        parsedTagMetadata.keys.retainAll(activeMediaIds)
    }

    fun clearCache() {
        parsedTagMetadata.clear()
    }
}
