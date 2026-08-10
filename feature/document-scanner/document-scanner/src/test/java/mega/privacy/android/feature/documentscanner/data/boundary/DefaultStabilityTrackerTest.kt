package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultStabilityTrackerTest {

    private lateinit var underTest: DefaultStabilityTracker

    @BeforeEach
    fun setUp() {
        underTest = DefaultStabilityTracker()
    }

    @Test
    fun `test that null result returns SEARCHING`() {
        val state = underTest.onDetectionResult(null)
        assertThat(state).isEqualTo(StabilityState.SEARCHING)
    }

    @Test
    fun `test that first detection returns UNSTABLE`() {
        val state = underTest.onDetectionResult(createResult())
        assertThat(state).isEqualTo(StabilityState.UNSTABLE)
    }

    @Test
    fun `test that null after detection resets to SEARCHING`() {
        underTest.onDetectionResult(createResult())
        val state = underTest.onDetectionResult(null)
        assertThat(state).isEqualTo(StabilityState.SEARCHING)
    }

    @Disabled(
        "STABILIZING is unreachable now that STABILIZING_FRAMES == STABLE_FRAMES: " +
            "first detection forces UNSTABLE, second detection hits STABLE directly. " +
            "The intermediate STABILIZING state was kept on the enum for legacy UI " +
            "branches but the tracker never returns it.",
    )
    @Test
    fun `test that identical boundaries reach STABILIZING after enough frames`() {
        val result = createResult()
        repeat(DefaultStabilityTracker.STABILIZING_FRAMES - 1) {
            underTest.onDetectionResult(result)
        }
        val state = underTest.onDetectionResult(result)
        assertThat(state).isEqualTo(StabilityState.STABILIZING)
    }

    @Test
    fun `test that identical boundaries reach STABLE after enough frames`() {
        val result = createResult()
        repeat(DefaultStabilityTracker.STABLE_FRAMES - 1) {
            underTest.onDetectionResult(result)
        }
        val state = underTest.onDetectionResult(result)
        assertThat(state).isEqualTo(StabilityState.STABLE)
    }

    @Test
    fun `test that large drift resets to UNSTABLE`() {
        val result1 = createResult(
            topLeft = Point(0.1f, 0.1f),
            topRight = Point(0.9f, 0.1f),
            bottomLeft = Point(0.1f, 0.9f),
            bottomRight = Point(0.9f, 0.9f),
        )
        val result2 = createResult(
            topLeft = Point(0.3f, 0.3f),
            topRight = Point(0.7f, 0.3f),
            bottomLeft = Point(0.3f, 0.7f),
            bottomRight = Point(0.7f, 0.7f),
        )

        // Build up to a stable state
        repeat(DefaultStabilityTracker.STABLE_FRAMES) {
            underTest.onDetectionResult(result1)
        }
        // Large shift
        val state = underTest.onDetectionResult(result2)
        assertThat(state).isEqualTo(StabilityState.UNSTABLE)
    }

    @Test
    fun `test that small drift within threshold keeps counting`() {
        val result1 = createResult(
            topLeft = Point(0.10f, 0.10f),
            topRight = Point(0.90f, 0.10f),
            bottomLeft = Point(0.10f, 0.90f),
            bottomRight = Point(0.90f, 0.90f),
        )
        val result2 = createResult(
            topLeft = Point(0.105f, 0.105f),
            topRight = Point(0.895f, 0.105f),
            bottomLeft = Point(0.105f, 0.895f),
            bottomRight = Point(0.895f, 0.895f),
        )

        repeat(DefaultStabilityTracker.STABLE_FRAMES - 1) { i ->
            val r = if (i % 2 == 0) result1 else result2
            underTest.onDetectionResult(r)
        }
        val state = underTest.onDetectionResult(result1)
        assertThat(state).isEqualTo(StabilityState.STABLE)
    }

    @Test
    fun `test that reset clears state`() {
        val result = createResult()
        repeat(DefaultStabilityTracker.STABLE_FRAMES) {
            underTest.onDetectionResult(result)
        }
        underTest.reset()
        val state = underTest.onDetectionResult(null)
        assertThat(state).isEqualTo(StabilityState.SEARCHING)
    }

    private fun createResult(
        topLeft: Point = Point(0.1f, 0.1f),
        topRight: Point = Point(0.9f, 0.1f),
        bottomLeft: Point = Point(0.1f, 0.9f),
        bottomRight: Point = Point(0.9f, 0.9f),
        confidence: Float = 0.8f,
        timestamp: Long = System.currentTimeMillis(),
    ): DetectionResult = DetectionResult(
        boundary = DocumentBoundary(
            topLeft = topLeft,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            confidence = confidence,
        ),
        frameTimestamp = timestamp,
        frameWidth = 480,
        frameHeight = 640,
    )
}
