package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YPlaneToGrayConverterTest {

    private val underTest = YPlaneToGrayConverter()

    @Test
    fun `test that a padding-free plane is copied verbatim`() {
        val plane = byteArrayOf(1, 2, 3, 4, 5, 6) // 3x2

        val result = underTest.convert(plane, width = 3, height = 2, rowStride = 3, pixelStride = 1)

        assertThat(result).isEqualTo(byteArrayOf(1, 2, 3, 4, 5, 6))
    }

    @Test
    fun `test that row padding is dropped`() {
        // 2x2 image with rowStride 4 (2 padding bytes per row).
        val plane = byteArrayOf(
            1, 2, 9, 9,
            3, 4, 9, 9,
        )

        val result = underTest.convert(plane, width = 2, height = 2, rowStride = 4, pixelStride = 1)

        assertThat(result).isEqualTo(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun `test that pixel stride is honoured`() {
        // 2x2 image, pixelStride 2 (interleaved luma), rowStride 4.
        val plane = byteArrayOf(
            1, 0, 2, 0,
            3, 0, 4, 0,
        )

        val result = underTest.convert(plane, width = 2, height = 2, rowStride = 4, pixelStride = 2)

        assertThat(result).isEqualTo(byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun `test that a rowStride smaller than the row width is rejected`() {
        val plane = ByteArray(6)
        assertThrows<IllegalArgumentException> {
            underTest.convert(plane, width = 3, height = 2, rowStride = 2, pixelStride = 1)
        }
    }
}
