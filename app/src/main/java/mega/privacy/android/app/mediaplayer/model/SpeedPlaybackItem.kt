package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.SPEED_PLAYBACK_0_5_X
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.SPEED_PLAYBACK_1_5_X
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.SPEED_PLAYBACK_1_X
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.SPEED_PLAYBACK_2_X

/**
 * The enum class for playback speed
 *
 * @property speed playback speed
 * @property iconId speed icon resource id
 */
enum class SpeedPlaybackItem(
    val speed: Float,
    @DrawableRes
    val iconId: Int,
) {
    /**
     * 0.5x playback speed
     */
    PLAYBACK_SPEED_0_5_X(speed = SPEED_PLAYBACK_0_5_X, iconId = R.drawable.ic_playback_0_5x),

    /**
     * 1x playback speed
     */
    PLAYBACK_SPEED_1_X(speed = SPEED_PLAYBACK_1_X, iconId = R.drawable.ic_playback_1x),

    /**
     * 1.5x playback speed
     */
    PLAYBACK_SPEED_1_5_X(speed = SPEED_PLAYBACK_1_5_X, iconId = R.drawable.ic_playback_1_5x),


    /**
     * 2x playback speed
     */
    PLAYBACK_SPEED_2_X(speed = SPEED_PLAYBACK_2_X, iconId = R.drawable.ic_playback_2x),
}
