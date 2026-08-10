package mega.privacy.android.feature.documentscanner.data.smoother

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExponentialMovingAverageBoundarySmootherTest {

    private lateinit var underTest: ExponentialMovingAverageBoundarySmoother

    @BeforeEach
    fun setUp() {
        underTest = ExponentialMovingAverageBoundarySmoother()
    }

    @Test
    fun `test that first frame returns the input unchanged`() {
        val input = boundary(tlX = 0.1f, tlY = 0.1f)

        val output = underTest.smooth(input)

        assertThat(output).isEqualTo(input)
    }

    @Test
    fun `test that second frame blends previous and current with ALPHA 0_7`() {
        // Keep the per-corner shift within SNAP_THRESHOLD (0.08) so the
        // smoother blends instead of snapping. 0.05 → distance ≈ 0.07.
        val first = boundary(tlX = 0.0f, tlY = 0.0f)
        val second = boundary(tlX = 0.05f, tlY = 0.05f)
        underTest.smooth(first)

        val output = underTest.smooth(second)

        // ALPHA = 0.7 → previous*0.7 + current*0.3 = 0.0*0.7 + 0.05*0.3 = 0.015
        assertThat(output.topLeft.x).isWithin(EPSILON).of(0.015f)
        assertThat(output.topLeft.y).isWithin(EPSILON).of(0.015f)
    }

    @Test
    fun `test that large jump beyond SNAP_THRESHOLD snaps to the new boundary`() {
        underTest.smooth(boundary(tlX = 0.0f, tlY = 0.0f))
        // SNAP_THRESHOLD = 0.08; bumping all four corners by 0.2 forces a snap.
        val jumped = boundary(
            tlX = 0.2f, tlY = 0.2f,
            trX = 0.9f, trY = 0.2f,
            brX = 0.9f, brY = 0.9f,
            blX = 0.2f, blY = 0.9f,
        )

        val output = underTest.smooth(jumped)

        assertThat(output).isEqualTo(jumped)
    }

    @Test
    fun `test that reset clears the previous frame so the next call snaps`() {
        underTest.smooth(boundary(tlX = 0.0f, tlY = 0.0f))

        underTest.reset()
        val nextFresh = boundary(tlX = 0.4f, tlY = 0.4f)
        val output = underTest.smooth(nextFresh)

        assertThat(output).isEqualTo(nextFresh)
    }

    @Test
    fun `test that confidence is copied from the latest frame, not the previous one`() {
        underTest.smooth(boundary(tlX = 0.0f, tlY = 0.0f, confidence = 1.0f))

        val output = underTest.smooth(boundary(tlX = 0.02f, tlY = 0.02f, confidence = 0.4f))

        assertThat(output.confidence).isEqualTo(0.4f)
    }

    private fun boundary(
        tlX: Float = 0.1f, tlY: Float = 0.1f,
        trX: Float = 0.9f, trY: Float = 0.1f,
        brX: Float = 0.9f, brY: Float = 0.9f,
        blX: Float = 0.1f, blY: Float = 0.9f,
        confidence: Float = 1f,
    ) = DocumentBoundary(
        topLeft = Point(tlX, tlY),
        topRight = Point(trX, trY),
        bottomRight = Point(brX, brY),
        bottomLeft = Point(blX, blY),
        confidence = confidence,
    )

    private companion object {
        const val EPSILON = 1e-4f
    }
}
