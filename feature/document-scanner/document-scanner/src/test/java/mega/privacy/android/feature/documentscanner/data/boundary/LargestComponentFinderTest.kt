package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargestComponentFinderTest {

    private lateinit var underTest: LargestComponentFinder

    @BeforeEach
    fun setUp() {
        underTest = LargestComponentFinder()
    }

    @Test
    fun `test that an empty mask returns null`() {
        val mask = FloatArray(WIDTH * HEIGHT)

        val result = underTest.findLargest(
            mask = mask,
            width = WIDTH,
            height = HEIGHT,
            threshold = THRESHOLD,
            minComponentPixels = 1,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that a single rectangular component returns its 4 extreme corners`() {
        // 4x3 rectangle at (2,1)..(5,3) inside a 10x6 mask.
        val mask = mask10x6 { x, y -> x in 2..5 && y in 1..3 }

        val result = underTest.findLargest(
            mask = mask,
            width = 10,
            height = 6,
            threshold = THRESHOLD,
            minComponentPixels = 1,
        )!!

        assertThat(result.componentSize).isEqualTo(4 * 3)
        assertThat(result.tl.toList()).containsExactly(2, 1).inOrder()
        assertThat(result.tr.toList()).containsExactly(5, 1).inOrder()
        assertThat(result.br.toList()).containsExactly(5, 3).inOrder()
        assertThat(result.bl.toList()).containsExactly(2, 3).inOrder()
    }

    @Test
    fun `test that the larger of two disjoint components is returned`() {
        // Small 2x2 at (0,0) and big 4x4 at (5,1).
        val mask = mask10x6 { x, y ->
            (x in 0..1 && y in 0..1) || (x in 5..8 && y in 1..4)
        }

        val result = underTest.findLargest(
            mask = mask,
            width = 10,
            height = 6,
            threshold = THRESHOLD,
            minComponentPixels = 1,
        )!!

        // Big rectangle is 4 * 4 = 16 pixels.
        assertThat(result.componentSize).isEqualTo(16)
        assertThat(result.tl.toList()).containsExactly(5, 1).inOrder()
        assertThat(result.br.toList()).containsExactly(8, 4).inOrder()
    }

    @Test
    fun `test that components below minComponentPixels are rejected`() {
        // Three foreground pixels at (0,0), (1,0), (0,1) → component size 3.
        val mask = mask10x6 { x, y -> x in 0..1 && y in 0..1 && !(x == 1 && y == 1) }

        val result = underTest.findLargest(
            mask = mask,
            width = 10,
            height = 6,
            threshold = THRESHOLD,
            minComponentPixels = 4,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that scratch buffers resize when called with a larger frame after a smaller one`() {
        val smallMask = mask10x6 { x, y -> x in 1..3 && y in 1..2 }
        underTest.findLargest(
            mask = smallMask, width = 10, height = 6,
            threshold = THRESHOLD, minComponentPixels = 1,
        )

        // Now feed a bigger mask — scratch buffers must grow.
        val bigWidth = 30
        val bigHeight = 20
        val bigMask = FloatArray(bigWidth * bigHeight) { idx ->
            val x = idx % bigWidth
            val y = idx / bigWidth
            if (x in 5..15 && y in 5..15) 1f else 0f
        }

        val result = underTest.findLargest(
            mask = bigMask,
            width = bigWidth,
            height = bigHeight,
            threshold = THRESHOLD,
            minComponentPixels = 1,
        )!!

        assertThat(result.componentSize).isEqualTo(11 * 11)
        assertThat(result.tl.toList()).containsExactly(5, 5).inOrder()
        assertThat(result.br.toList()).containsExactly(15, 15).inOrder()
    }

    @Test
    fun `test that a mask shorter than width times height returns null`() {
        val shortMask = FloatArray(4) { 1f }

        val result = underTest.findLargest(
            mask = shortMask,
            width = 10,
            height = 6,
            threshold = THRESHOLD,
            minComponentPixels = 1,
        )

        assertThat(result).isNull()
    }

    private inline fun mask10x6(predicate: (x: Int, y: Int) -> Boolean): FloatArray {
        val out = FloatArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                out[y * WIDTH + x] = if (predicate(x, y)) 1f else 0f
            }
        }
        return out
    }

    private companion object {
        const val WIDTH = 10
        const val HEIGHT = 6
        const val THRESHOLD = 0.5f
    }
}
