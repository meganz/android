package mega.privacy.android.feature.documentscanner.data.boundary

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Validates a candidate document boundary against geometric quality guards
 * before it propagates downstream. Extracted from the detector so the
 * thresholds and reasoning can be reviewed (and unit tested) in isolation.
 *
 * Two checks:
 * - **Fill ratio**: how much of the bounding quad's area is actually filled
 *   by the detected mask. A clean document mask fills ~98% of its quad; a
 *   hand resting on the page carves a notch out and drops the ratio to
 *   0.6-0.8. Rejected below [MIN_FILL_RATIO].
 * - **Opposite-side ratio**: how close in length the parallel quad sides are.
 *   Severely trapezoidal detections (e.g. one corner mis-pinned to a hand)
 *   produce very unequal sides. Rejected below [MIN_OPPOSITE_SIDE_RATIO].
 */
class BoundaryGuard @Inject constructor() {

    enum class RejectReason { ZERO_AREA, FILL_RATIO, WIDTH_RATIO, HEIGHT_RATIO }

    sealed interface Verdict {
        data object Accepted : Verdict
        data class Rejected(val reason: RejectReason) : Verdict
    }

    /**
     * Geometric metrics for a candidate quad along with the [Verdict].
     * The detector logs these in one go so a single line per analysis frame
     * captures the full guard rationale.
     */
    data class EvaluatedQuad(
        val verdict: Verdict,
        val quadArea: Int,
        val fillRatio: Float,
        val widthRatio: Double,
        val heightRatio: Double,
        val maskCoverage: Float,
    )

    fun evaluate(
        tl: IntArray,
        tr: IntArray,
        br: IntArray,
        bl: IntArray,
        componentSize: Int,
        maskWidth: Int,
        maskHeight: Int,
    ): EvaluatedQuad {
        val quadArea = quadAreaPx(tl, tr, br, bl)
        val fillRatio = if (quadArea > 0) componentSize / quadArea.toFloat() else 0f
        val topLen = distancePx(tl, tr)
        val bottomLen = distancePx(bl, br)
        val leftLen = distancePx(tl, bl)
        val rightLen = distancePx(tr, br)
        val widthRatio = sideRatio(topLen, bottomLen)
        val heightRatio = sideRatio(leftLen, rightLen)
        val frameArea = (maskWidth * maskHeight).toFloat()
        val maskCoverage = if (frameArea > 0f) componentSize / frameArea else 0f

        // Returns the FIRST failing guard in this order, not an exhaustive
        // list — a quad that fails several checks is reported by whichever
        // fires first. Fine for our use (we only need a yes/no + one reason
        // to log).
        val verdict: Verdict = when {
            quadArea <= 0 -> Verdict.Rejected(RejectReason.ZERO_AREA)
            fillRatio < MIN_FILL_RATIO -> Verdict.Rejected(RejectReason.FILL_RATIO)
            widthRatio < MIN_OPPOSITE_SIDE_RATIO -> Verdict.Rejected(RejectReason.WIDTH_RATIO)
            heightRatio < MIN_OPPOSITE_SIDE_RATIO -> Verdict.Rejected(RejectReason.HEIGHT_RATIO)
            else -> Verdict.Accepted
        }

        return EvaluatedQuad(
            verdict = verdict,
            quadArea = quadArea,
            fillRatio = fillRatio,
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            maskCoverage = maskCoverage,
        )
    }

    private fun distancePx(a: IntArray, b: IntArray): Double {
        val dx = (a[0] - b[0]).toDouble()
        val dy = (a[1] - b[1]).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    private fun sideRatio(a: Double, b: Double): Double {
        if (a <= 0.0 || b <= 0.0) return 0.0
        return if (a < b) a / b else b / a
    }

    /** Polygon area via the shoelace formula. Corners passed clockwise. */
    private fun quadAreaPx(
        tl: IntArray,
        tr: IntArray,
        br: IntArray,
        bl: IntArray,
    ): Int {
        val sum = (tl[0] * tr[1] - tr[0] * tl[1]) +
                (tr[0] * br[1] - br[0] * tr[1]) +
                (br[0] * bl[1] - bl[0] * br[1]) +
                (bl[0] * tl[1] - tl[0] * bl[1])
        return abs(sum) / 2
    }

    companion object {
        // Hand-on-document guard: a clean document mask should fill ~98%+ of
        // its bounding quad. A hand carves a notch out, dropping the ratio to
        // 0.6-0.8. 0.88 catches the typical "hand resting on page" case while
        // leaving room for the slight irregularity of a curved page edge.
        const val MIN_FILL_RATIO = 0.88f

        // Opposite-side-length guard: rejects severely trapezoidal detections.
        // 0.55 is what real-device logs show for held-by-hand scans where the
        // phone tilts noticeably; tighter values (0.70) reject most legit
        // phone-tilt detections and leave the user stuck on "hold steady".
        const val MIN_OPPOSITE_SIDE_RATIO = 0.55
    }
}
