package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [AudioPlayerGradientBackground].
 *
 * The gradient is an overlay, so each golden draws it over a flat colour: against a plain
 * background the opaque and translucent variants are hard to tell apart, and the direction the
 * gradient runs in is not visible at all.
 */
class AudioPlayerGradientBackgroundScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GradientOpaqueReversed() {
        AndroidThemeForPreviews {
            Box(modifier = Modifier.size(200.dp).background(Color.Red)) {
                AudioPlayerGradientBackground(
                    modifier = Modifier.fillMaxSize(),
                    reversed = true,
                    opaque = true,
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GradientTranslucentReversed() {
        AndroidThemeForPreviews {
            Box(modifier = Modifier.size(200.dp).background(Color.Red)) {
                AudioPlayerGradientBackground(
                    modifier = Modifier.fillMaxSize(),
                    reversed = true,
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun GradientTranslucentForward() {
        AndroidThemeForPreviews {
            Box(modifier = Modifier.size(200.dp).background(Color.Red)) {
                AudioPlayerGradientBackground(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
