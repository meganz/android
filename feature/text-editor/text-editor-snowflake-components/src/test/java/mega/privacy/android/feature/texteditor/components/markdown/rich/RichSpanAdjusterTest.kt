package mega.privacy.android.feature.texteditor.components.markdown.rich

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.texteditor.components.markdown.rich.RichSpanAdjuster.TextChange
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RichSpanAdjusterTest {

    private val bold = RichSpanStyle.Bold
    private val italic = RichSpanStyle.Italic

    private fun insertion(at: Int, length: Int) = TextChange(at, at, at, at + length)

    // ---- adjust: insertions ----

    @Test
    fun `test that adjust grows a span when typing strictly inside it`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(insertion(at = 3, length = 2)),
        )

        assertThat(result).containsExactly(RichSpan(2, 7, bold))
    }

    @Test
    fun `test that adjust keeps a span when typing at its end without the typing style`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(insertion(at = 5, length = 2)),
        )

        assertThat(result).containsExactly(RichSpan(2, 5, bold))
    }

    @Test
    fun `test that adjust grows a span when typing at its end with the typing style active`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(insertion(at = 5, length = 2)),
            typingStyles = setOf(bold),
        )

        assertThat(result).containsExactly(RichSpan(2, 7, bold))
    }

    @Test
    fun `test that adjust shifts a span when typing at its start without the typing style`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(insertion(at = 2, length = 2)),
        )

        assertThat(result).containsExactly(RichSpan(4, 7, bold))
    }

    @Test
    fun `test that adjust creates a span when typing styled text into plain text`() {
        val result = RichSpanAdjuster.adjust(
            emptyList(),
            listOf(insertion(at = 3, length = 4)),
            typingStyles = setOf(bold, italic),
        )

        assertThat(result).containsExactly(
            RichSpan(3, 7, bold),
            RichSpan(3, 7, italic),
        )
    }

    // ---- adjust: deletions and replacements ----

    @Test
    fun `test that adjust shrinks a span when deleting inside it`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 8, bold)),
            listOf(TextChange(4, 6, 4, 4)),
        )

        assertThat(result).containsExactly(RichSpan(2, 6, bold))
    }

    @Test
    fun `test that adjust drops a span whose whole content is deleted`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(3, 5, bold)),
            listOf(TextChange(2, 6, 2, 2)),
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that adjust keeps the style when retyping an exactly selected span`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(TextChange(2, 5, 2, 8)),
        )

        assertThat(result).containsExactly(RichSpan(2, 8, bold))
    }

    @Test
    fun `test that adjust clips a span when a replacement crosses its boundary`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 5, bold)),
            listOf(TextChange(4, 7, 4, 6)),
        )

        assertThat(result).containsExactly(RichSpan(2, 4, bold))
    }

    @Test
    fun `test that adjust remaps spans through multiple changes in one edit`() {
        val result = RichSpanAdjuster.adjust(
            listOf(RichSpan(2, 4, bold), RichSpan(8, 10, italic)),
            listOf(insertion(at = 0, length = 1), TextChange(6, 6, 7, 9)),
        )

        assertThat(result).containsExactly(
            RichSpan(3, 5, bold),
            RichSpan(11, 13, italic),
        ).inOrder()
    }

    // ---- toggle ----

    @Test
    fun `test that toggle applies a style over plain text`() {
        val result = RichSpanAdjuster.toggle(emptyList(), 1, 4, bold)

        assertThat(result).containsExactly(RichSpan(1, 4, bold))
    }

    @Test
    fun `test that toggle removes a fully covered range and splits the span`() {
        val result = RichSpanAdjuster.toggle(listOf(RichSpan(2, 8, bold)), 4, 6, bold)

        assertThat(result).containsExactly(
            RichSpan(2, 4, bold),
            RichSpan(6, 8, bold),
        ).inOrder()
    }

    @Test
    fun `test that toggle extends a partially covered range`() {
        val result = RichSpanAdjuster.toggle(listOf(RichSpan(2, 5, bold)), 4, 8, bold)

        assertThat(result).containsExactly(RichSpan(2, 8, bold))
    }

    @Test
    fun `test that toggle treats adjacent same-style spans as covering`() {
        val result = RichSpanAdjuster.toggle(
            listOf(RichSpan(2, 4, bold), RichSpan(4, 6, bold)),
            3,
            5,
            bold,
        )

        assertThat(result).containsExactly(
            RichSpan(2, 3, bold),
            RichSpan(5, 6, bold),
        ).inOrder()
    }

    @Test
    fun `test that normalize merges overlapping same-style spans and drops empty ones`() {
        val result = RichSpanAdjuster.normalize(
            listOf(
                RichSpan(4, 6, bold),
                RichSpan(2, 5, bold),
                RichSpan(3, 3, italic),
                RichSpan(2, 5, italic),
            ),
        )

        assertThat(result).containsExactly(
            RichSpan(2, 6, bold),
            RichSpan(2, 5, italic),
        ).inOrder()
    }
}
