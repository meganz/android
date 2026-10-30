package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class MarkdownTooLargeBannerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun MarkdownTooLargeBanner() {
        AndroidThemeForPreviews {
            MarkdownTooLargeBanner(onDismiss = {})
        }
    }
}
