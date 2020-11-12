package mega.privacy.android.app.audioplayer

/**
 * This class hold metadata for an audio node.
 *
 * @property title the track name
 * @property artist the artist name
 * @property album the album name
 * @property nodeName the node name
 */
data class Metadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val nodeName: String,
)
