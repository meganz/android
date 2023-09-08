package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.VideoPlayerFragment.Companion.SPEED_PLAYBACK_1_X

/**
 * The speed playback entity
 *
 * @property speed playback speed
 * @property iconId speed icon resource id
 */
data class SpeedPlaybackItem(
    val speed: Float = SPEED_PLAYBACK_1_X,
    @DrawableRes
    val iconId: Int = R.drawable.ic_playback_1x,
)
