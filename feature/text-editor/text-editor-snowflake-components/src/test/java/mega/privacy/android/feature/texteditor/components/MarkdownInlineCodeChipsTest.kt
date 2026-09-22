package mega.privacy.android.feature.texteditor.components

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichSpan
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichSpanStyle
import org.junit.Test

class MarkdownInlineCodeChipsTest {

    private val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace)
    private val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)

    @Test
    fun `test that an annotated string without code yields no ranges`() {
        val text = buildAnnotatedString {
            append("plain ")
            withStyle(boldStyle) { append("bold") }
        }

        assertThat(text.inlineCodeRanges(codeStyle)).isEmpty()
    }

    @Test
    fun `test that every code run of an annotated string yields a range`() {
        val text = buildAnnotatedString {
            append("run ")
            withStyle(codeStyle) { append("one") }
            append(" then ")
            withStyle(boldStyle) { append("bold") }
            withStyle(codeStyle) { append("two") }
        }

        assertThat(text.inlineCodeRanges(codeStyle))
            .containsExactly(TextRange(4, 7), TextRange(17, 20))
            .inOrder()
    }

    @Test
    fun `test that a code run nested in another style still yields a range`() {
        val text = buildAnnotatedString {
            withStyle(boldStyle) {
                append("a")
                withStyle(codeStyle) { append("code") }
            }
        }

        assertThat(text.inlineCodeRanges(codeStyle)).containsExactly(TextRange(1, 5))
    }

    @Test
    fun `test that editor spans without code yield no ranges`() {
        val spans = listOf(
            RichSpan(0, 4, RichSpanStyle.Bold),
            RichSpan(5, 9, RichSpanStyle.Link("https://mega.io")),
        )

        assertThat(spans.inlineCodeRanges()).isEmpty()
    }

    @Test
    fun `test that every code span of a block yields a range`() {
        val spans = listOf(
            RichSpan(0, 4, RichSpanStyle.Code),
            RichSpan(4, 8, RichSpanStyle.Bold),
            RichSpan(8, 12, RichSpanStyle.Code),
        )

        assertThat(spans.inlineCodeRanges())
            .containsExactly(TextRange(0, 4), TextRange(8, 12))
            .inOrder()
    }
}
