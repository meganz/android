package mega.privacy.android.feature.documentscanner.presentation.analyzer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanFrameAnalyzerTest {

    // 2x2, no padding: identity conversion so we can assert on the bytes.
    private fun plane() = byteArrayOf(1, 2, 3, 4)

    private fun analyze(underTest: ScanFrameAnalyzer, timestampMs: Long, onBytes: () -> Unit = {}) =
        underTest.analyze(
            width = 2, height = 2, rowStride = 2, pixelStride = 1,
            rotationDegrees = 90, timestampMs = timestampMs,
        ) {
            onBytes()
            plane()
        }

    @Test
    fun `test that a processed frame returns the converted grayscale frame`() {
        val underTest = ScanFrameAnalyzer(intervalMs = 200)

        val frame = analyze(underTest, timestampMs = 1_000)

        assertThat(frame).isNotNull()
        assertThat(frame!!.bytes).isEqualTo(byteArrayOf(1, 2, 3, 4))
        assertThat(frame.width).isEqualTo(2)
        assertThat(frame.height).isEqualTo(2)
        assertThat(frame.rotationDegrees).isEqualTo(90)
        assertThat(frame.timestampMs).isEqualTo(1_000)
    }

    @Test
    fun `test that a throttled frame returns null`() {
        val underTest = ScanFrameAnalyzer(intervalMs = 200)
        analyze(underTest, timestampMs = 1_000)

        assertThat(analyze(underTest, timestampMs = 1_100)).isNull()
    }

    @Test
    fun `test that the plane bytes provider is not invoked for a throttled frame`() {
        val underTest = ScanFrameAnalyzer(intervalMs = 200)
        analyze(underTest, timestampMs = 1_000)

        var providerCalled = false
        analyze(underTest, timestampMs = 1_100) { providerCalled = true }

        assertThat(providerCalled).isFalse()
    }

    @Test
    fun `test that a frame past the interval is processed again`() {
        val underTest = ScanFrameAnalyzer(intervalMs = 200)
        analyze(underTest, timestampMs = 1_000)

        assertThat(analyze(underTest, timestampMs = 1_200)).isNotNull()
    }
}
