package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for [computeReadThroughFraction]. A "line" is 100px tall in these cases, so the line
 * count visible at the bottom of the viewport is easy to reason about.
 */
class TextEditorReadThroughTest {

    @Test
    fun `test that fraction is zero when the document has no lines`() {
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 1,
            chunkSizePx = 0,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 4000,
            totalLines = 0,
        )

        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that fraction is zero when the chunk has not been measured yet`() {
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 46,
            chunkSizePx = 0,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 4000,
            totalLines = 45,
        )

        assertThat(result).isEqualTo(0f)
    }

    @Test
    fun `test that a single chunk shown from the top is below the read-through threshold`() {
        // 45 lines * 100px = 4500px; viewport shows the first 40 lines (4000px).
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 46,
            chunkSizePx = 4500,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 4000,
            totalLines = 45,
        )

        assertThat(result).isWithin(0.001f).of(40f / 45f)
        assertThat(result).isLessThan(0.9f)
    }

    @Test
    fun `test that a single chunk scrolled to its last line is fully read`() {
        // Scrolled up by 500px so the 4500px chunk's bottom aligns with the 4000px viewport.
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 46,
            chunkSizePx = 4500,
            chunkOffsetPx = -500,
            viewportEndOffsetPx = 4000,
            totalLines = 45,
        )

        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that a file shorter than the viewport is fully read`() {
        // 45 lines * 100px = 4500px fully visible within a 5000px viewport.
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 46,
            chunkSizePx = 4500,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 5000,
            totalLines = 45,
        )

        assertThat(result).isEqualTo(1f)
    }

    @Test
    fun `test that the read-through threshold is reached at ninety percent of the lines`() {
        // 100 lines * 100px; viewport bottom at 9000px exposes line 90.
        val result = computeReadThroughFraction(
            chunkStartLine = 1,
            nextChunkStartLine = 101,
            chunkSizePx = 10000,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 9000,
            totalLines = 100,
        )

        assertThat(result).isEqualTo(0.9f)
    }

    @Test
    fun `test that a mid-document non-uniform chunk uses its real line span`() {
        // 2500-line file. The last visible chunk spans lines 1001-1400 (400 lines, packed by
        // character capacity, NOT a fixed chunk size) and is 40000px tall -> 100px/line. Shown
        // from its top, the viewport (4000px) exposes 40 lines -> line 1040 of 2500.
        val result = computeReadThroughFraction(
            chunkStartLine = 1001,
            nextChunkStartLine = 1401,
            chunkSizePx = 40_000,
            chunkOffsetPx = 0,
            viewportEndOffsetPx = 4000,
            totalLines = 2500,
        )

        assertThat(result).isWithin(0.001f).of(1040f / 2500f)
        assertThat(result).isLessThan(0.9f)
    }
}
