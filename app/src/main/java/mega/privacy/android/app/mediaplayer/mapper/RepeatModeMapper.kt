package mega.privacy.android.app.mediaplayer.mapper

import com.google.android.exoplayer2.Player
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode

/**
 * Mapper to convert RepeatToggleMode to repeat mode of ExoPlayer
 */
typealias RepeatModeMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards RepeatToggleMode

internal fun toRepeatToggleModeMapper(repeatMode: Int): RepeatToggleMode =
    when (repeatMode) {
        Player.REPEAT_MODE_OFF -> RepeatToggleMode.REPEAT_NONE
        Player.REPEAT_MODE_ONE -> RepeatToggleMode.REPEAT_ONE
        else -> RepeatToggleMode.REPEAT_ALL
    }