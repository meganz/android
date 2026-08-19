package mega.privacy.android.feature.mediaplayer.data.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.util.UUID
import javax.inject.Inject
import mega.privacy.android.domain.entity.node.TypedAudioNode
import mega.privacy.android.feature.mediaplayer.data.MediaHandleStore

/**
 * Maps a [TypedAudioNode] and a resolved stream [Uri] to a Media3 [MediaItem].
 *
 * When displayName is provided it is stored as [MediaMetadata.displayTitle] — deliberately NOT
 * [MediaMetadata.title]. In the combined player metadata, fields set on the [MediaItem] take
 * precedence over metadata extracted from embedded tags (ID3, Vorbis comment, etc.), so setting
 * `title` here would permanently hide the real song title. Keeping the file name in
 * `displayTitle` lets consumers fall back to it while the `title` slot stays reserved for the
 * tag-derived value (see AudioMediaControllerFacade's title resolution).
 *
 * Artwork is resolved separately by the Compose UI layer via ThumbnailRequest.
 */
class AudioNodeToMediaItemMapper @Inject constructor(
    private val mediaHandleStore: MediaHandleStore,
) {

    operator fun invoke(node: TypedAudioNode, uri: Uri): MediaItem =
        invoke(handle = node.id.longValue, uri = uri, displayName = node.name)

    /**
     * Create a [MediaItem] directly from raw fields (for offline items and the fast first-emit).
     *
     * @param displayName optional file name shown before embedded metadata is extracted
     */
    operator fun invoke(handle: Long, uri: Uri, displayName: String? = null): MediaItem {
        val mediaId = UUID.randomUUID().toString()
        mediaHandleStore.register(mediaId, handle)
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(mediaId)
            .apply {
                if (displayName != null) {
                    setMediaMetadata(MediaMetadata.Builder().setDisplayTitle(displayName).build())
                }
            }
            .build()
    }
}
