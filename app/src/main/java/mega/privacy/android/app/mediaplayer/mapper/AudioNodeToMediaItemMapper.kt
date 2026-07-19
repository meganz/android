package mega.privacy.android.app.mediaplayer.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import mega.privacy.android.domain.entity.node.TypedAudioNode
import javax.inject.Inject

/**
 * Maps a [TypedAudioNode] and a resolved stream [Uri] to a Media3 [MediaItem].
 *
 * When [displayName] is provided it is set as the initial [MediaMetadata.title] so the UI can
 * show the file name immediately while Media3 extracts embedded tags (ID3, Vorbis comment, etc.).
 * Once extraction completes, Media3 fires Player.Listener.onMediaMetadataChanged with the real
 * title/artist, which takes precedence over the initial value.
 * Artwork is resolved separately by the Compose UI layer via ThumbnailRequest.
 */
class AudioNodeToMediaItemMapper @Inject constructor() {

    /**
     * Create a [MediaItem] from a [TypedAudioNode] and its resolved stream [Uri].
     * The node's [TypedAudioNode.name] is used as the initial title fallback.
     */
    operator fun invoke(node: TypedAudioNode, uri: Uri): MediaItem =
        invoke(handle = node.id.longValue, uri = uri, displayName = node.name)

    /**
     * Create a [MediaItem] directly from raw fields (for offline items and the fast first-emit).
     *
     * @param displayName optional file name shown before embedded metadata is extracted
     */
    operator fun invoke(handle: Long, uri: Uri, displayName: String? = null): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(handle.toString())
            .apply {
                if (displayName != null) {
                    setMediaMetadata(MediaMetadata.Builder().setTitle(displayName).build())
                }
            }
            .build()
}
