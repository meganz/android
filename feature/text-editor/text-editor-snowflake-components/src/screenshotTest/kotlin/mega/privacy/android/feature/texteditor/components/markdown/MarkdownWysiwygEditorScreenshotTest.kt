package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Screenshot tests for the WYSIWYG Markdown live-styling transformation: raw Markdown source in
 * an editable text field with content styled and syntax delimiters dimmed (Phase A — markers
 * stay visible).
 */
class MarkdownWysiwygEditorScreenshotTest {

    private val fixture = """
        # Heading one
        ## Heading two

        Plain text with **bold**, *italic*, ~~strikethrough~~ and `inline code`.

        - First bullet
        - Second bullet with [a link](https://mega.io)

        1. Ordered item
        2. Another item

        > A block quote line

        ```kotlin
        val answer = 42
        ```

        ---
    """.trimIndent()

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun WysiwygStyledEditor() {
        AndroidThemeForPreviews {
            BasicTextField(
                state = TextFieldState(fixture),
                textStyle = TextStyle(
                    color = DSTokens.colors.text.primary
                ),
                outputTransformation = rememberMarkdownWysiwygOutputTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
