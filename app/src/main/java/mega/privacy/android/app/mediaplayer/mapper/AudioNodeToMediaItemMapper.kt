package mega.privacy.android.app.mediaplayer.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import mega.privacy.android.domain.entity.node.TypedAudioNode
import javax.inject.Inject

/**
 * Maps a [TypedAudioNode] and a resolved stream [Uri] to a Media3 [MediaItem].
 *
 * The [MediaItem] carries only the URI and mediaId (node handle). Title and other
 * metadata are left unset so Media3 extracts them from the file's embedded tags
 * (ID3, Vorbis comment, etc.), which takes precedence over the node filename.
 * Artwork is resolved separately by the Compose UI layer via ThumbnailRequest.
 */
class AudioNodeToMediaItemMapper @Inject constructor() {

    /**
     * Create a [MediaItem] from a [TypedAudioNode] and its resolved stream [Uri].
     */
    operator fun invoke(node: TypedAudioNode, uri: Uri): MediaItem =
        invoke(handle = node.id.longValue, uri = uri)

    /**
     * Create a [MediaItem] directly from raw fields (for offline items and the fast first-emit).
     */
    operator fun invoke(handle: Long, uri: Uri): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(handle.toString())
            .build()
}
