package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                onTextStyleSelected = {},
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
                    isUnderline = true,
                    headingLevel = 2,
                    isBulletList = true,
                ),
                onAction = {},
                onTextStyleSelected = {},
            )
        }
    }

    /**
     * The actions that scroll off the right edge at phone width (quote, code, link), which the
     * goldens above cut off. Over-wide on purpose: the preview canvas keeps the device width,
     * so the row overflows and the capture lands on its trailing end.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ToolbarTrailingActions() {
        AndroidThemeForPreviews {
            MarkdownFormattingToolbar(
                formats = MarkdownSelectionFormats.Empty,
                onAction = {},
                onTextStyleSelected = {},
                modifier = Modifier.requiredWidth(680.dp),
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

    /** The whole dialog, including its title and the Remove link action. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LinkDialogExistingLink() {
        AndroidThemeForPreviews {
            MarkdownLinkDialog(
                initialText = "MEGA",
                initialUrl = "https://mega.io",
                showRemove = true,
                onConfirm = { _, _ -> },
                onRemove = {},
                onDismiss = {},
            )
        }
    }
}
