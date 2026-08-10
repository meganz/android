package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrayFrameRotatorTest {

    private lateinit var underTest: GrayFrameRotator

    @BeforeEach
    fun setUp() {
        underTest = GrayFrameRotator()
    }

    /**
     * Fixture: 2-wide, 3-tall frame
     *
     *     [ 1, 2 ]
     *     [ 3, 4 ]
     *     [ 5, 6 ]
     */
    private val source = byteArrayOf(1, 2, 3, 4, 5, 6)

    @Test
    fun `test that zero degrees returns the input bytes unchanged`() {
        val rotated = underTest.rotate(source, width = 2, height = 3, degrees = 0)

        assertThat(rotated.bytes).isSameInstanceAs(source)
        assertThat(rotated.width).isEqualTo(2)
        assertThat(rotated.height).isEqualTo(3)
    }

    @Test
    fun `test that 90 degrees rotates clockwise and swaps width and height`() {
        val rotated = underTest.rotate(source, width = 2, height = 3, degrees = 90)

        // Expected:
        // [ 5, 3, 1 ]
        // [ 6, 4, 2 ]
        assertThat(rotated.width).isEqualTo(3)
        assertThat(rotated.height).isEqualTo(2)
        assertThat(rotated.bytes.toList()).containsExactly(
            5.toByte(), 3.toByte(), 1.toByte(),
            6.toByte(), 4.toByte(), 2.toByte(),
        ).inOrder()
    }

    @Test
    fun `test that 180 degrees flips both axes and keeps dimensions`() {
        val rotated = underTest.rotate(source, width = 2, height = 3, degrees = 180)

        // Expected: reversed bytes, same dims
        assertThat(rotated.width).isEqualTo(2)
        assertThat(rotated.height).isEqualTo(3)
        assertThat(rotated.bytes.toList()).containsExactly(
            6.toByte(), 5.toByte(),
            4.toByte(), 3.toByte(),
            2.toByte(), 1.toByte(),
        ).inOrder()
    }

    @Test
    fun `test that 270 degrees rotates counter-clockwise and swaps width and height`() {
        val rotated = underTest.rotate(source, width = 2, height = 3, degrees = 270)

        // Expected:
        // [ 2, 4, 6 ]
        // [ 1, 3, 5 ]
        assertThat(rotated.width).isEqualTo(3)
        assertThat(rotated.height).isEqualTo(2)
        assertThat(rotated.bytes.toList()).containsExactly(
            2.toByte(), 4.toByte(), 6.toByte(),
            1.toByte(), 3.toByte(), 5.toByte(),
        ).inOrder()
    }

    @Test
    fun `test that an unsupported angle is treated as a no-op`() {
        val rotated = underTest.rotate(source, width = 2, height = 3, degrees = 45)

        assertThat(rotated.bytes).isSameInstanceAs(source)
        assertThat(rotated.width).isEqualTo(2)
        assertThat(rotated.height).isEqualTo(3)
    }
}
