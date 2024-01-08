package mega.privacy.android.app.mediaplayer.mapper

import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import javax.inject.Inject

/**
 * Mapper to convert repeat mode of ExoPlayer to RepeatToggleMode
 */
class ExoPlayerRepeatModeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param repeatToggleMode RepeatToggleMode
     * @return Repeat mode of ExoPlayer based on RepeatToggleMode
     */
    operator fun invoke(repeatToggleMode: RepeatToggleMode) =
        when (repeatToggleMode) {
            RepeatToggleMode.REPEAT_ONE -> REPEAT_MODE_ONE
            RepeatToggleMode.REPEAT_NONE -> REPEAT_MODE_OFF
            else -> REPEAT_MODE_ALL
        }
}