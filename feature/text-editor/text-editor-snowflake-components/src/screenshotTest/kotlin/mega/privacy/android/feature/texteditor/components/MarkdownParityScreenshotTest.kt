package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.texteditor.components.markdown.rich.MarkdownToRichDocumentConverter
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichDocumentEditor
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichDocumentState

/**
 * View/Edit parity harness: the SAME fixture rendered by the read-only preview blocks (top)
 * and the rich editor (bottom) in one golden, so any visual drift between the two surfaces
 * shows up as an asymmetry inside a single image.
 */
class MarkdownParityScreenshotTest {

    private val textFixture = """
        # Trip notes

        Planning the **spring** trip with *flexible* dates, `inline code`, <u>underline</u>, <u>~~both lines~~</u> and a [booking link](https://mega.io).
        This hard-wrapped line renders as a line break in both surfaces.

        - Book flights
        - [x] Reserve hotel
        - [ ] Compare prices

        1. Pack bags
        2. Fly out

        > Remember to check passport expiry.
        >
        > Also pack the chargers.
    """.trimIndent()

    private val boxFixture = """
        Budget so far:

        ```kotlin
        val budget = 1200
        ```

        | City | Nights |
        |------|--------|
        | Rome | 3 |
    """.trimIndent()

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PreviewVersusRichEditor() {
        ParityStack(fixture = textFixture, editorHeight = 620.dp)
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun PreviewVersusRichEditorBoxes() {
        ParityStack(fixture = boxFixture, editorHeight = 360.dp)
    }

    @Composable
    private fun ParityStack(fixture: String, editorHeight: Dp) {
        AndroidThemeForPreviews {
            Column {
                val blocks = remember { parseTopLevelBlocks(fixture) }
                val colors = rememberMarkdownColors()
                // Same horizontal padding the editor applies via contentPadding, so the two
                // halves of the image align column-for-column.
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    blocks.forEach { MarkdownBlock(it, colors) }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                val richState = remember {
                    RichDocumentState(MarkdownToRichDocumentConverter().convert(fixture))
                }
                Box(modifier = Modifier.height(editorHeight)) {
                    RichDocumentEditor(state = richState)
                }
            }
        }
    }
}
