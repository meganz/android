package mega.privacy.android.app.mediaplayer.playlist

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import java.io.File

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
 */
data class PlaylistItem(
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File?,
    var index: Int,
    val type: Int,
    val size: Long,
    var isSelected: Boolean = false,
    var headerIsVisible: Boolean = false
) {
    /**
     * Create a new instance with the specified index and item type,
     * and nullify thumbnail if it's not exist.
     *
     * @param index new index
     * @param type item type
     * @param isSelected Whether the item is selected
     * @return the new instance
     */
    fun finalizeItem(index: Int, type: Int, isSelected: Boolean = false): PlaylistItem {
        return PlaylistItem(
            nodeHandle, nodeName,
            if (thumbnail?.exists() == true) thumbnail else null,
            index, type, size, isSelected
        )
    }

    /**
     * Create a new instance with new node name.
     *
     * @param newName new node name
     * @return the new instance
     */
    fun updateNodeName(newName: String) =
        PlaylistItem(nodeHandle, newName, thumbnail, index, type, size, isSelected)

    companion object {
        const val TYPE_PREVIOUS = 1
        const val TYPE_PLAYING = 2
        const val TYPE_NEXT = 3

        /**
         * Get name of item header
         * @param type item type
         * @param paused media whether is paused
         * @return the name of header
         */
        fun getHeaderName(type: Int, paused: Boolean = false): String {
            return getString(
                when (type) {
                    TYPE_PREVIOUS -> R.string.general_previous
                    TYPE_NEXT -> R.string.general_next
                    else -> {
                        if (paused) {
                            R.string.audio_player_now_playing_paused
                        } else {
                            R.string.audio_player_now_playing
                        }
                    }
                }
            )
        }
    }
}
