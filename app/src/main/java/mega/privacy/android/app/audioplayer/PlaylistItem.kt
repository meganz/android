package mega.privacy.android.app.audioplayer

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File

data class PlaylistItem(
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File?,
    val index: Int,
    var type: Int,
) {
    companion object {
        const val TYPE_PREVIOUS = 1
        const val TYPE_PREVIOUS_HEADER = 2
        const val TYPE_PLAYING = 3
        const val TYPE_PLAYING_HEADER = 4
        const val TYPE_NEXT = 5
        const val TYPE_NEXT_HEADER = 6

        fun headerItem(context: Context, type: Int, paused: Boolean = false): PlaylistItem {
            val name = context.getString(
                when (type) {
                    TYPE_PREVIOUS_HEADER -> R.string.general_previous
                    TYPE_NEXT_HEADER -> R.string.general_next
                    else -> {
                        if (paused) {
                            R.string.audio_player_now_playing_paused
                        } else {
                            R.string.audio_player_now_playing
                        }
                    }
                }
            )
            return PlaylistItem(INVALID_HANDLE, name, null, INVALID_VALUE, type)
        }
    }
}
