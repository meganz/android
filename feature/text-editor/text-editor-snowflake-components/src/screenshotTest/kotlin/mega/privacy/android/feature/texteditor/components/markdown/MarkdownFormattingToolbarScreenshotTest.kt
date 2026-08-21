package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the Markdown formatting toolbar and the link dialog body, providing
 * Weblate translators with the surrounding UI context for the toolbar and dialog strings.
 */
class MarkdownFormattingToolbarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ToolbarNoActiveFormats() {
        AndroidThemeForPreviews {
            MarkdownFormattingToolbar(
                formats = MarkdownSelectionFormats.Empty,
                onAction = {},
                showModeSwitch = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ToolbarActiveFormats() {
        AndroidThemeForPreviews {
            MarkdownFormattingToolbar(
                formats = MarkdownSelectionFormats(
                    isBold = true,
                    isItalic = true,
                    headingLevel = 2,
                    isBulletList = true,
                ),
                onAction = {},
                showModeSwitch = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LinkDialogContent() {
        AndroidThemeForPreviews {
            MarkdownLinkDialogContent(
                linkText = "MEGA",
                url = "https://mega.io",
                onLinkTextChanged = {},
                onUrlChanged = {},
            )
        }
    }
}
