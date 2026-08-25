package mega.privacy.android.feature.chat.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import kotlin.time.Duration.Companion.seconds

/**
 * Screenshot tests for the stateless [ReturnToCallBanner], covering the label-only variant and the
 * variant with an elapsed-time chronometer.
 */
class ReturnToCallBannerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReturnToCallBannerWithoutTimer() {
        AndroidThemeForPreviews {
            ReturnToCallBanner(
                text = "Tap to return to call",
                elapsed = null,
                onClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReturnToCallBannerWithTimer() {
        AndroidThemeForPreviews {
            ReturnToCallBanner(
                text = "Tap to return to call",
                elapsed = 125.seconds,
                onClick = {},
            )
        }
    }
}
