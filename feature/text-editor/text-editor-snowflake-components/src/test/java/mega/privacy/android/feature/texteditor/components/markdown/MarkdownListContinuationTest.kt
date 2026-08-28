package mega.privacy.android.feature.texteditor.components.markdown

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.texteditor.components.markdown.MarkdownListContinuation.Marker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarkdownListContinuationTest {

    @Test
    fun `test that markerOf parses a bullet marker`() {
        assertThat(MarkdownListContinuation.markerOf("- item")).isEqualTo(Marker(2, "- "))
        assertThat(MarkdownListContinuation.markerOf("* item")).isEqualTo(Marker(2, "* "))
        assertThat(MarkdownListContinuation.markerOf("+ item")).isEqualTo(Marker(2, "+ "))
    }

    @Test
    fun `test that markerOf preserves indentation in the continuation`() {
        assertThat(MarkdownListContinuation.markerOf("  - item")).isEqualTo(Marker(4, "  - "))
    }

    @Test
    fun `test that markerOf increments ordered markers`() {
        assertThat(MarkdownListContinuation.markerOf("3. item")).isEqualTo(Marker(3, "4. "))
        assertThat(MarkdownListContinuation.markerOf("9) item")).isEqualTo(Marker(3, "10) "))
    }

    @Test
    fun `test that markerOf parses quote markers`() {
        assertThat(MarkdownListContinuation.markerOf("> quoted")).isEqualTo(Marker(2, "> "))
        assertThat(MarkdownListContinuation.markerOf(">")).isEqualTo(Marker(1, "> "))
    }

    @Test
    fun `test that markerOf resets task boxes to unchecked in the continuation`() {
        assertThat(MarkdownListContinuation.markerOf("- [ ] task")).isEqualTo(Marker(6, "- [ ] "))
        assertThat(MarkdownListContinuation.markerOf("- [x] done")).isEqualTo(Marker(6, "- [ ] "))
        assertThat(MarkdownListContinuation.markerOf("2. [x] done"))
            .isEqualTo(Marker(7, "3. [ ] "))
    }

    @Test
    fun `test that markerOf returns null when there is no marker`() {
        assertThat(MarkdownListContinuation.markerOf("plain")).isNull()
        assertThat(MarkdownListContinuation.markerOf("-nospace")).isNull()
        assertThat(MarkdownListContinuation.markerOf("1.nospace")).isNull()
        assertThat(MarkdownListContinuation.markerOf("")).isNull()
        assertThat(MarkdownListContinuation.markerOf("   ")).isNull()
    }

    @Test
    fun `test that isEmptyItem is true only for a bare marker`() {
        assertThat(MarkdownListContinuation.isEmptyItem("- ")).isTrue()
        assertThat(MarkdownListContinuation.isEmptyItem("1. ")).isTrue()
        assertThat(MarkdownListContinuation.isEmptyItem("> ")).isTrue()
        assertThat(MarkdownListContinuation.isEmptyItem("- [ ] ")).isTrue()
        assertThat(MarkdownListContinuation.isEmptyItem("- x")).isFalse()
        assertThat(MarkdownListContinuation.isEmptyItem("plain")).isFalse()
    }
}
