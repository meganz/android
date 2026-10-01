package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [VideoPlayerGestureSlider].
 *
 * The icon is selected by a `when` over the value, so every branch gets its own golden: brightness
 * uses a single icon, while volume picks between four depending on how loud it is.
 *
 * The two upper volume values sit exactly on their branch thresholds. A comparison that loosens
 * from `<` to `<=` drops them into the branch below and changes the icon, which a value taken
 * from the middle of a branch would not reveal.
 */
class VideoPlayerGestureSliderScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun Brightness() {
        AndroidThemeForPreviews {
            VideoPlayerGestureSlider(value = 0.6f, type = GestureSliderType.Brightness)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VolumeMuted() {
        AndroidThemeForPreviews {
            VideoPlayerGestureSlider(value = 0f, type = GestureSliderType.Volume)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VolumeLow() {
        AndroidThemeForPreviews {
            VideoPlayerGestureSlider(value = 0.1f, type = GestureSliderType.Volume)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VolumeMedium() {
        AndroidThemeForPreviews {
            VideoPlayerGestureSlider(value = 0.2f, type = GestureSliderType.Volume)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VolumeHigh() {
        AndroidThemeForPreviews {
            VideoPlayerGestureSlider(value = 0.5f, type = GestureSliderType.Volume)
        }
    }
}
