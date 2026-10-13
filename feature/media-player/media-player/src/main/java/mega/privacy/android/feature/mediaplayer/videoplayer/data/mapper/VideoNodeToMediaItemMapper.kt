package mega.privacy.android.feature.mediaplayer.videoplayer.data.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import javax.inject.Inject

/**
 * Maps the fields of a video node to the [MediaItem] the player takes.
 *
 * The node handle is the media id, unlike the audio mapper's random UUID and lookup store, which
 * lets the facade read the handle straight back off a player transition.
 *
 * The file name goes in `displayTitle` rather than `title` for the reason the audio mapper gives:
 * fields set here win over anything read out of the container, so using `title` would hide the
 * real one for good.
 */
class VideoNodeToMediaItemMapper @Inject constructor() {

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
