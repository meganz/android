package mega.privacy.android.feature.texteditor.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

class TextEditorErrorContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TextEditorErrorContent() {
        AndroidThemeForPreviews {
            TextEditorErrorContent(
                description = stringResource(sharedR.string.file_preview_error_description),
                onRetry = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TextEditorErrorContentWithoutRetry() {
        AndroidThemeForPreviews {
            TextEditorErrorContent(
                description = stringResource(sharedR.string.file_link_expired_title),
                onRetry = null,
            )
        }
    }
}
