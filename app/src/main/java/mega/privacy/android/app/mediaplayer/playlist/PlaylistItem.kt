package mega.privacy.android.app.mediaplayer.playlist

import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * UI data class for playlist screen.
 *
 * @property nodeHandle node handle
 * @property nodeName node name
 * @property thumbnail thumbnail file path, null if not available
 * @property index the index used for seek to this item
 * @property type item type
 * @property size size of the node
 * @property isSelected Whether the item is selected
 * @property headerIsVisible the header of item if is visible
 * @property duration the duration of audio
 */
data class PlaylistItem(
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File?,
    val index: Int,
    val type: Int,
    val size: Long,
    val isSelected: Boolean = false,
    val headerIsVisible: Boolean = false,
    val duration: Duration = 0.seconds,
)
