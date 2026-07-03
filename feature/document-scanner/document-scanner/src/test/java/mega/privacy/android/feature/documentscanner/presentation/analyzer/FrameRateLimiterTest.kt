package mega.privacy.android.feature.documentscanner.presentation.analyzer

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FrameRateLimiterTest {

    @Test
    fun `test that the first frame is always processed`() {
        val underTest = FrameRateLimiter(intervalMs = 200)
        assertThat(underTest.shouldProcess(0)).isTrue()
    }

    @Test
    fun `test that frames within the interval are skipped`() {
        val underTest = FrameRateLimiter(intervalMs = 200)
        underTest.shouldProcess(1_000)

        assertThat(underTest.shouldProcess(1_100)).isFalse()
        assertThat(underTest.shouldProcess(1_199)).isFalse()
    }

    @Test
    fun `test that a frame at or past the interval is processed`() {
        val underTest = FrameRateLimiter(intervalMs = 200)
        underTest.shouldProcess(1_000)

        assertThat(underTest.shouldProcess(1_200)).isTrue()
    }

    @Test
    fun `test that the interval is measured from the last processed frame not the last seen`() {
        val underTest = FrameRateLimiter(intervalMs = 200)
        underTest.shouldProcess(1_000)   // processed
        underTest.shouldProcess(1_150)   // skipped, does not reset the clock

        assertThat(underTest.shouldProcess(1_201)).isTrue()
    }

    @Test
    fun `test that reset makes the next frame process immediately`() {
        val underTest = FrameRateLimiter(intervalMs = 200)
        underTest.shouldProcess(1_000)
        underTest.reset()

        assertThat(underTest.shouldProcess(1_050)).isTrue()
    }
}
