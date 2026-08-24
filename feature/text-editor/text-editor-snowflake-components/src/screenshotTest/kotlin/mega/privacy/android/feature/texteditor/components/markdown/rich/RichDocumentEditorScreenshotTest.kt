package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the rich editor: every block directly editable with structural chrome
 * (bullets, numbers, checkboxes, quote bars, code boxes) and span-styled inline text — no
 * Markdown syntax anywhere.
 */
class RichDocumentEditorScreenshotTest {

    private val document = RichDocument(
        listOf(
            RichBlock.Heading(1, RichText("Trip notes")),
            RichBlock.Paragraph(
                RichText(
                    "Planning the spring trip with flexible dates.",
                    listOf(
                        RichSpan(13, 19, RichSpanStyle.Bold),
                        RichSpan(30, 38, RichSpanStyle.Italic),
                    ),
                ),
            ),
            RichBlock.ListItem(
                ordered = false,
                indent = 0,
                checked = null,
                RichText("Book flights")
            ),
            RichBlock.ListItem(
                ordered = false,
                indent = 0,
                checked = true,
                RichText("Reserve hotel")
            ),
            RichBlock.ListItem(
                ordered = false,
                indent = 1,
                checked = false,
                RichText("Compare prices")
            ),
            RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("Pack bags")),
            RichBlock.ListItem(ordered = true, indent = 0, checked = null, RichText("Fly out")),
            RichBlock.Quote(1, RichText("Remember to check passport expiry.")),
            RichBlock.CodeBlock("kotlin", "val budget = 1200"),
            RichBlock.ThematicBreak,
            RichBlock.RawSource("| City | Nights |\n|------|--------|\n| Rome | 3 |"),
        ),
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RichEditorBlocks() {
        AndroidThemeForPreviews {
            RichDocumentEditor(state = remember { RichDocumentState(document) })
        }
    }
}
