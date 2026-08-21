package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownWysiwygOutputTransformationTest {

    @get:Rule
    var composeRule = createComposeRule()

    private var layoutResult: TextLayoutResult? = null
    private var dimColor: Color = Color.Unspecified
    private var h1FontSize: TextUnit = TextUnit.Unspecified
    private var h1LineHeight: TextUnit = TextUnit.Unspecified

    @Composable
    private fun TransformedField(text: String) {
        dimColor = DSTokens.colors.text.secondary
        h1FontSize = MaterialTheme.typography.headlineSmall.fontSize
        h1LineHeight = MaterialTheme.typography.headlineSmall.lineHeight
        BasicTextField(
            state = TextFieldState(text),
            outputTransformation = rememberMarkdownWysiwygOutputTransformation(),
            onTextLayout = { getResult -> getResult()?.let { layoutResult = it } },
        )
    }

    private fun setContent(text: String): TextLayoutResult {
        composeRule.setContent {
            AndroidThemeForPreviews {
                TransformedField(text)
            }
        }
        composeRule.waitForIdle()
        return requireNotNull(layoutResult) { "text layout was not produced" }
    }

    @Test
    fun `test that transformOutput styles bold content without changing the text`() {
        val layout = setContent("a **b** c")

        assertThat(layout.layoutInput.text.text).isEqualTo("a **b** c")
        val boldSpan = layout.layoutInput.text.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold
        }
        assertThat(boldSpan.any { it.start == 4 && it.end == 5 }).isTrue()
    }

    @Test
    fun `test that transformOutput dims syntax delimiters`() {
        val layout = setContent("a **b** c")

        val delimiterSpans = layout.layoutInput.text.spanStyles.filter {
            it.item.color == dimColor
        }
        assertThat(delimiterSpans.any { it.start == 2 && it.end == 4 }).isTrue()
        assertThat(delimiterSpans.any { it.start == 5 && it.end == 7 }).isTrue()
    }

    @Test
    fun `test that transformOutput enlarges the whole heading line and sets its line height`() {
        val layout = setContent("# Title\nbody")

        val text = layout.layoutInput.text
        assertThat(text.text).isEqualTo("# Title\nbody")
        val headingSpan = text.spanStyles.filter { it.item.fontSize == h1FontSize }
        assertThat(headingSpan.any { it.start == 0 && it.end == 7 }).isTrue()
        val headingParagraph = text.paragraphStyles.filter { it.item.lineHeight == h1LineHeight }
        assertThat(headingParagraph.any { it.start == 0 && it.end == 7 }).isTrue()
    }

    @Test
    fun `test that transformOutput styles inline code with a monospace background`() {
        val layout = setContent("a `c` b")

        val codeSpans = layout.layoutInput.text.spanStyles.filter {
            it.item.fontFamily == FontFamily.Monospace && it.item.background != Color.Unspecified
        }
        assertThat(codeSpans.any { it.start == 3 && it.end == 4 }).isTrue()
    }

    @Test
    fun `test that transformOutput leaves plain text unstyled`() {
        val layout = setContent("plain text only")

        assertThat(layout.layoutInput.text.spanStyles).isEmpty()
        assertThat(layout.layoutInput.text.paragraphStyles).isEmpty()
    }
}
