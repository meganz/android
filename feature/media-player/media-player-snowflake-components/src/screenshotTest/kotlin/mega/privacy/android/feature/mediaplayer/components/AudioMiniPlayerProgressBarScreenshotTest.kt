package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [AudioMiniPlayerProgressBar]. The bar fills the available width, so a fixed
 * width is imposed to keep the played fraction comparable between goldens.
 *
 * The empty and full ends are covered because the played fraction is clamped, and a clamp that
 * stops working is only visible at the boundaries.
 */
class AudioMiniPlayerProgressBarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ProgressEmpty() {
        AndroidThemeForPreviews {
            AudioMiniPlayerProgressBar(progress = 0f, modifier = Modifier.width(240.dp))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ProgressPartial() {
        AndroidThemeForPreviews {
            AudioMiniPlayerProgressBar(progress = 0.35f, modifier = Modifier.width(240.dp))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ProgressComplete() {
        AndroidThemeForPreviews {
            AudioMiniPlayerProgressBar(progress = 1f, modifier = Modifier.width(240.dp))
        }
    }
}
