package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes

/**
 * The interface for playback speed
 *
 * @property speed playback speed
 * @property iconId speed icon resource id
 */
interface SpeedPlaybackItem {
    val speed: Float

    @get: DrawableRes
    val iconId: Int
}