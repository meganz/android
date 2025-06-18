package mega.privacy.android.app.mediaplayer.model

import androidx.annotation.DrawableRes

/**
 * The interface for playback speed
 *
 * @property speed playback speed
 * @property text speed text
 */
interface SpeedPlaybackItem {
    val speed: Float
    val text: String
}