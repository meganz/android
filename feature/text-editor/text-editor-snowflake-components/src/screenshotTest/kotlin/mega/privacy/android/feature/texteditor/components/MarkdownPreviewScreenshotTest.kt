package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot test for the read-only Markdown block rendering, covering the chrome it shares
 * with the rich editor ([MarkdownListItemFrame]/[MarkdownQuoteFrame]/[MarkdownCodeFrame]): list
 * markers, the full-height quote bar, and the code box, plus inline styling. Renders the parsed
 * blocks directly — [MarkdownPreview] itself parses asynchronously and would screenshot its
 * loading state.
 */
class MarkdownPreviewScreenshotTest {

    private val content = """
        # Trip notes

        Planning the **spring** trip with *flexible* dates.

        - Book flights
        - Reserve hotel

        1. Pack bags
        2. Fly out

        > Remember to check passport expiry.

        ```kotlin
        val budget = 1200
        ```

        ---

        Use `inline code` and ~~strike~~ with a [link](https://mega.io).
    """.trimIndent()

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PreviewBlockChrome() {
        AndroidThemeForPreviews {
            val blocks = remember { parseTopLevelBlocks(content) }
            val colors = rememberMarkdownColors()
            Column {
                blocks.forEach { MarkdownBlock(it, colors) }
            }
        }
    }
}
