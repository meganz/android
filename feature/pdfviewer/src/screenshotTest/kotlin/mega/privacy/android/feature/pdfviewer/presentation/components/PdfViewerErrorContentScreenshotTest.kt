package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class PdfViewerErrorContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PdfViewerErrorContent() {
        AndroidThemeForPreviews {
            PdfViewerErrorContent(onRetry = {})
        }
    }
}
