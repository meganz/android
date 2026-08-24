package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RichTextBlockStateTest {

    @Test
    fun `test that linkAt finds the link under a collapsed caret`() {
        val state = RichTextBlockState(
            "see docs here",
            listOf(RichSpan(4, 8, RichSpanStyle.Link("https://mega.io"))),
        )

        assertThat(state.linkAt(TextRange(6))).isNotNull()
        assertThat(state.linkAt(TextRange(8))).isNotNull()
        assertThat(state.linkAt(TextRange(4))).isNull()
        assertThat(state.linkAt(TextRange(10))).isNull()
    }

    @Test
    fun `test that linkAt finds a link the selection overlaps`() {
        val state = RichTextBlockState(
            "see docs here",
            listOf(RichSpan(4, 8, RichSpanStyle.Link("https://mega.io"))),
        )

        assertThat(state.linkAt(TextRange(0, 6))).isNotNull()
        assertThat(state.linkAt(TextRange(4, 8))).isNotNull()
        assertThat(state.linkAt(TextRange(0, 4))).isNull()
    }

    @Test
    fun `test that applyLink wraps the selection in a link span`() {
        val state = RichTextBlockState("hello world")

        state.applyLink("hello", "https://mega.io", TextRange(0, 5))

        assertThat(state.textFieldState.text.toString()).isEqualTo("hello world")
        assertThat(state.spans)
            .containsExactly(RichSpan(0, 5, RichSpanStyle.Link("https://mega.io")))
    }

    @Test
    fun `test that applyLink inserts the link text at a collapsed caret`() {
        val state = RichTextBlockState("ab")

        state.applyLink("mega", "https://mega.io", TextRange(1))

        assertThat(state.textFieldState.text.toString()).isEqualTo("amegab")
        assertThat(state.spans)
            .containsExactly(RichSpan(1, 5, RichSpanStyle.Link("https://mega.io")))
        assertThat(state.textFieldState.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun `test that applyLink falls back to the url when the text is blank`() {
        val state = RichTextBlockState("")

        state.applyLink("", "https://mega.io", TextRange(0))

        assertThat(state.textFieldState.text.toString()).isEqualTo("https://mega.io")
    }

    @Test
    fun `test that applyLink replaces an existing link's text and url`() {
        val state = RichTextBlockState(
            "see docs here",
            listOf(RichSpan(4, 8, RichSpanStyle.Link("https://old"))),
        )

        state.applyLink("manual", "https://new", TextRange(6))

        assertThat(state.textFieldState.text.toString()).isEqualTo("see manual here")
        assertThat(state.spans)
            .containsExactly(RichSpan(4, 10, RichSpanStyle.Link("https://new")))
    }

    @Test
    fun `test that applyLink remaps other spans through the text change`() {
        val state = RichTextBlockState(
            "link and bold",
            listOf(
                RichSpan(0, 4, RichSpanStyle.Link("https://old")),
                RichSpan(9, 13, RichSpanStyle.Bold),
            ),
        )

        state.applyLink("linked", "https://new", TextRange(2))

        assertThat(state.textFieldState.text.toString()).isEqualTo("linked and bold")
        assertThat(state.spans).containsExactly(
            RichSpan(0, 6, RichSpanStyle.Link("https://new")),
            RichSpan(11, 15, RichSpanStyle.Bold),
        )
    }

    @Test
    fun `test that applyLink does nothing when the url is blank`() {
        val state = RichTextBlockState("hello")

        state.applyLink("hello", " ", TextRange(0, 5))

        assertThat(state.spans).isEmpty()
    }

    @Test
    fun `test that removeLink unwraps the link and keeps its text`() {
        val state = RichTextBlockState(
            "see docs here",
            listOf(RichSpan(4, 8, RichSpanStyle.Link("https://mega.io"))),
        )

        state.removeLink(TextRange(6))

        assertThat(state.textFieldState.text.toString()).isEqualTo("see docs here")
        assertThat(state.spans).isEmpty()
    }

    @Test
    fun `test that removeLink does nothing without a targeted link`() {
        val spans = listOf(RichSpan(0, 3, RichSpanStyle.Bold))
        val state = RichTextBlockState("see docs", spans)

        state.removeLink(TextRange(1))

        assertThat(state.spans).isEqualTo(spans)
    }
}
