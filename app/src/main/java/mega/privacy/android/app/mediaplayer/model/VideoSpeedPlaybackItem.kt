package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R

/**
 * The enum class for playback speed
 *
 * @property speed playback speed
 * @property iconId speed icon resource id
 */
enum class VideoSpeedPlaybackItem(
    override val speed: Float,
    @DrawableRes override val iconId: Int,
) : SpeedPlaybackItem {
    /**
     * 0.5x playback speed
     */
    PLAYBACK_SPEED_0_5_X(speed = 0.5F, iconId = R.drawable.ic_playback_0_5x),

    /**
     * 1x playback speed
     */
    PLAYBACK_SPEED_1_X(speed = 1F, iconId = R.drawable.ic_playback_1x),

    /**
     * 1.5x playback speed
     */
    PLAYBACK_SPEED_1_5_X(speed = 1.5F, iconId = R.drawable.ic_playback_1_5x),


    /**
     * 2x playback speed
     */
    PLAYBACK_SPEED_2_X(speed = 2F, iconId = R.drawable.ic_playback_2x),
}
