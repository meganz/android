package mega.privacy.android.feature.mediaplayer.videoplayer.data.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import mega.privacy.android.domain.entity.node.TypedVideoNode
import javax.inject.Inject

/**
 * Maps a video node, or the raw fields of one, to the [MediaItem] the player takes.
 *
 * The node handle is the media id, unlike the audio mapper's random UUID and lookup store. Two
 * things fall out of that: the facade can read the handle straight back off a transition, and the
 * id for a given video is the same every time it is built, so a queue that arrives after playback
 * has started extends the timeline rather than replacing it.
 *
 * The file name goes in `displayTitle` rather than `title` for the reason the audio mapper gives:
 * fields set here win over anything read out of the container, so using `title` would hide the
 * real one for good.
 */
class VideoNodeToMediaItemMapper @Inject constructor() {

    operator fun invoke(node: TypedVideoNode, uri: Uri): MediaItem =
        invoke(handle = node.id.longValue, uri = uri, displayName = node.name)

    operator fun invoke(handle: Long, uri: Uri, displayName: String? = null): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(handle.toString())
            .apply {
                if (displayName != null) {
                    setMediaMetadata(MediaMetadata.Builder().setDisplayTitle(displayName).build())
                }
            }
            .build()
}
