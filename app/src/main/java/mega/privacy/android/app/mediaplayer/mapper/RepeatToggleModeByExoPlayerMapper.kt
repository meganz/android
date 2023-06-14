package mega.privacy.android.app.mediaplayer.mapper

import com.google.android.exoplayer2.Player
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import javax.inject.Inject

/**
 * The mapper class for converting the data of ExoPlayer entity to [RepeatToggleMode]
 */
class RepeatToggleModeByExoPlayerMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param repeatMode Int value of repeat mode of ExoPlayer
     * @return RepeatToggleMode
     */
    operator fun invoke(repeatMode: Int) =
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> RepeatToggleMode.REPEAT_NONE
            Player.REPEAT_MODE_ONE -> RepeatToggleMode.REPEAT_ONE
            else -> RepeatToggleMode.REPEAT_ALL
        }
}