package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoundaryGuardTest {

    private lateinit var underTest: BoundaryGuard

    @BeforeEach
    fun setUp() {
        underTest = BoundaryGuard()
    }

    @Test
    fun `test that a clean square quad is accepted`() {
        // 100x100 quad inside a 200x200 mask with 100% fill.
        val side = 100
        val componentSize = side * side
        val verdict = underTest.evaluate(
            tl = intArrayOf(50, 50),
            tr = intArrayOf(150, 50),
            br = intArrayOf(150, 150),
            bl = intArrayOf(50, 150),
            componentSize = componentSize,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(BoundaryGuard.Verdict.Accepted)
        assertThat(verdict.fillRatio).isWithin(EPSILON).of(1f)
    }

    @Test
    fun `test that a zero-area quad is rejected with ZERO_AREA`() {
        // All four corners collapsed to one point.
        val verdict = underTest.evaluate(
            tl = intArrayOf(50, 50),
            tr = intArrayOf(50, 50),
            br = intArrayOf(50, 50),
            bl = intArrayOf(50, 50),
            componentSize = 0,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.ZERO_AREA),
        )
    }

    @Test
    fun `test that low fill ratio is rejected with FILL_RATIO`() {
        // 100x100 quad but only 50% filled — hand-on-page signature.
        val quadArea = 100 * 100
        val verdict = underTest.evaluate(
            tl = intArrayOf(50, 50),
            tr = intArrayOf(150, 50),
            br = intArrayOf(150, 150),
            bl = intArrayOf(50, 150),
            componentSize = quadArea / 2,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.FILL_RATIO),
        )
        assertThat(verdict.fillRatio).isLessThan(BoundaryGuard.MIN_FILL_RATIO)
    }

    @Test
    fun `test that severely trapezoidal width is rejected with WIDTH_RATIO`() {
        // Top edge 100 px, bottom edge 10 px → widthRatio = 0.1, well below 0.55.
        // Fill ratio stays high so only the width guard fires.
        val tl = intArrayOf(0, 0)
        val tr = intArrayOf(100, 0)
        val br = intArrayOf(55, 100)
        val bl = intArrayOf(45, 100)
        // Triangle-ish area = (100 + 10) / 2 * 100 = 5500
        val verdict = underTest.evaluate(
            tl, tr, br, bl,
            componentSize = 5500,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.WIDTH_RATIO),
        )
    }

    @Test
    fun `test that severely trapezoidal height is rejected with HEIGHT_RATIO`() {
        // Left edge 100 px, right edge 10 px → heightRatio = 0.1.
        val tl = intArrayOf(0, 0)
        val tr = intArrayOf(100, 45)
        val br = intArrayOf(100, 55)
        val bl = intArrayOf(0, 100)
        // Quad area ≈ (100 + 10) / 2 * 100 = 5500
        val verdict = underTest.evaluate(
            tl, tr, br, bl,
            componentSize = 5500,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.HEIGHT_RATIO),
        )
    }

    @Test
    fun `test that fill ratio just above the threshold is accepted`() {
        // 100x100 quad (area 10000), 8900 filled → 0.89 ≥ 0.88.
        val verdict = underTest.evaluate(
            tl = intArrayOf(50, 50),
            tr = intArrayOf(150, 50),
            br = intArrayOf(150, 150),
            bl = intArrayOf(50, 150),
            componentSize = 8900,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(BoundaryGuard.Verdict.Accepted)
    }

    @Test
    fun `test that fill ratio just below the threshold is rejected`() {
        // 100x100 quad (area 10000), 8700 filled → 0.87 < 0.88.
        val verdict = underTest.evaluate(
            tl = intArrayOf(50, 50),
            tr = intArrayOf(150, 50),
            br = intArrayOf(150, 150),
            bl = intArrayOf(50, 150),
            componentSize = 8700,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.FILL_RATIO),
        )
    }

    @Test
    fun `test that opposite side ratio just above the threshold is accepted`() {
        // Top edge 100, bottom edge 60 → widthRatio 0.60 ≥ 0.55. Square-ish
        // height keeps the height guard well clear, full fill keeps fill clear.
        val tl = intArrayOf(0, 0)
        val tr = intArrayOf(100, 0)
        val br = intArrayOf(80, 100)
        val bl = intArrayOf(20, 100)
        // Shoelace area = (100 + 60) / 2 * 100 = 8000
        val verdict = underTest.evaluate(
            tl, tr, br, bl,
            componentSize = 8000,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(BoundaryGuard.Verdict.Accepted)
    }

    @Test
    fun `test that opposite side ratio just below the threshold is rejected`() {
        // Top edge 100, bottom edge 50 → widthRatio 0.50 < 0.55.
        val tl = intArrayOf(0, 0)
        val tr = intArrayOf(100, 0)
        val br = intArrayOf(75, 100)
        val bl = intArrayOf(25, 100)
        // Shoelace area = (100 + 50) / 2 * 100 = 7500
        val verdict = underTest.evaluate(
            tl, tr, br, bl,
            componentSize = 7500,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.verdict).isEqualTo(
            BoundaryGuard.Verdict.Rejected(BoundaryGuard.RejectReason.WIDTH_RATIO),
        )
    }

    @Test
    fun `test that mask coverage is reported even when accepted`() {
        // Square mask filling exactly 25% of the frame.
        val side = 100
        val verdict = underTest.evaluate(
            tl = intArrayOf(0, 0),
            tr = intArrayOf(side, 0),
            br = intArrayOf(side, side),
            bl = intArrayOf(0, side),
            componentSize = side * side,
            maskWidth = 200,
            maskHeight = 200,
        )
        assertThat(verdict.maskCoverage).isWithin(EPSILON).of(0.25f)
    }

    private companion object {
        const val EPSILON = 1e-4f
    }
}
