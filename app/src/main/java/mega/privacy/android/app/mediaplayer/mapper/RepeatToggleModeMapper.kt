package mega.privacy.android.app.mediaplayer.mapper

import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.Player.REPEAT_MODE_OFF
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import mega.privacy.android.app.mediaplayer.model.RepeatToggleMode

/**
 * Mapper to convert repeat mode of ExoPlayer to RepeatToggleMode
 */
typealias RepeatToggleModeMapper = (@JvmSuppressWildcards RepeatToggleMode) -> @JvmSuppressWildcards Int

internal fun toRepeatModeMapper(repeatToggleMode: RepeatToggleMode): Int =
    when (repeatToggleMode) {
        RepeatToggleMode.REPEAT_ONE -> REPEAT_MODE_ONE
        RepeatToggleMode.REPEAT_NONE -> REPEAT_MODE_OFF
        else -> REPEAT_MODE_ALL
    }