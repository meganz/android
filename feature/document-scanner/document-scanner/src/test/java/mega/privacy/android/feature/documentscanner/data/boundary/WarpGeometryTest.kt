package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.cos
import kotlin.math.sin

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarpGeometryTest {

    @Test
    fun `test that toPixelQuad scales normalised corners to the image size`() {
        val boundary = boundary(
            topLeft = Point(0.0f, 0.0f),
            topRight = Point(1.0f, 0.0f),
            bottomRight = Point(1.0f, 1.0f),
            bottomLeft = Point(0.0f, 1.0f),
        )

        val quad = boundary.toPixelQuad(srcWidth = 400, srcHeight = 300)

        assertThat(quad.topLeft).isEqualTo(Point(0f, 0f))
        assertThat(quad.topRight).isEqualTo(Point(400f, 0f))
        assertThat(quad.bottomRight).isEqualTo(Point(400f, 300f))
        assertThat(quad.bottomLeft).isEqualTo(Point(0f, 300f))
    }

    @Test
    fun `test that toPixelQuad maps an inset quad to the correct pixels`() {
        val boundary = boundary(
            topLeft = Point(0.1f, 0.2f),
            topRight = Point(0.9f, 0.2f),
            bottomRight = Point(0.9f, 0.8f),
            bottomLeft = Point(0.1f, 0.8f),
        )

        val quad = boundary.toPixelQuad(srcWidth = 1000, srcHeight = 500)

        assertThat(quad.topLeft).isEqualTo(Point(100f, 100f))
        assertThat(quad.topRight).isEqualTo(Point(900f, 100f))
        assertThat(quad.bottomRight).isEqualTo(Point(900f, 400f))
        assertThat(quad.bottomLeft).isEqualTo(Point(100f, 400f))
    }

    @Test
    fun `test that warpTargetSize equals the rectangle dimensions for an axis-aligned quad`() {
        val quad = boundary(
            topLeft = Point(0.0f, 0.0f),
            topRight = Point(1.0f, 0.0f),
            bottomRight = Point(1.0f, 1.0f),
            bottomLeft = Point(0.0f, 1.0f),
        ).toPixelQuad(srcWidth = 640, srcHeight = 480)

        val (width, height) = quad.warpTargetSize(imageWidth = 640, imageHeight = 480)

        assertThat(width).isEqualTo(640)
        assertThat(height).isEqualTo(480)
    }

    @Test
    fun `test that warpTargetSize keeps the edge-ratio for a sheared (affine) quad`() {
        // A parallelogram: no perspective (k2==k3==1), so the ratio is the edge
        // ratio — width edge 400, height edge 200 → 2:1.
        val quad = PixelQuad(
            topLeft = Point(100f, 100f),
            topRight = Point(500f, 100f),
            bottomRight = Point(560f, 300f),
            bottomLeft = Point(160f, 300f),
        )

        val (width, height) = quad.warpTargetSize(imageWidth = 1000, imageHeight = 1000)

        // width edge = 400; height edge = hypot(60,200) ≈ 208.8; ratio ≈ 1.916.
        assertThat(width.toFloat() / height).isWithin(0.02f).of(400f / 208.8f)
    }

    @Test
    fun `test that warpTargetSize recovers the true aspect of a perspective-projected page`() {
        // Project a real A4-portrait rectangle (210 x 297) through a pinhole camera
        // tilted 25 deg, then confirm the estimator recovers ~210:297 despite the
        // foreshortening (the old max-edge heuristic collapsed this toward square).
        val w = 210f
        val h = 297f
        val quad = PixelQuad(
            topLeft = project(-w / 2, h / 2),
            topRight = project(w / 2, h / 2),
            bottomRight = project(w / 2, -h / 2),
            bottomLeft = project(-w / 2, -h / 2),
        )

        val (width, height) = quad.warpTargetSize(imageWidth = IMAGE_SIZE, imageHeight = IMAGE_SIZE)

        assertThat(width.toFloat() / height).isWithin(0.05f).of(w / h)
    }

    @Test
    fun `test that warpTargetSize clamps a degenerate quad to at least one pixel`() {
        val quad = PixelQuad(
            topLeft = Point(50f, 50f),
            topRight = Point(50f, 50f),
            bottomRight = Point(50f, 50f),
            bottomLeft = Point(50f, 50f),
        )

        val (width, height) = quad.warpTargetSize(imageWidth = 100, imageHeight = 100)

        assertThat(width).isEqualTo(1)
        assertThat(height).isEqualTo(1)
    }

    /**
     * Pinhole projection of a point on a plane tilted [TILT_DEGREES] about the x-axis,
     * into a centred [IMAGE_SIZE]×[IMAGE_SIZE] image. y is up in plane space, down in
     * image space.
     */
    private fun project(x: Float, y: Float): Point {
        val tx = Math.toRadians(TILT_DEGREES)
        val ty = Math.toRadians(TILT_Y_DEGREES)
        // Rotate about x-axis.
        val x0 = x.toDouble()
        val y0 = y * cos(tx)
        val z0 = y * sin(tx)
        // Then about y-axis, so neither pair of opposite edges stays parallel.
        val xr = x0 * cos(ty) + z0 * sin(ty)
        val zr = -x0 * sin(ty) + z0 * cos(ty)
        val depth = zr + DISTANCE
        val u = FOCAL * xr / depth + IMAGE_SIZE / 2.0
        val v = IMAGE_SIZE / 2.0 - FOCAL * y0 / depth
        return Point(u.toFloat(), v.toFloat())
    }

    private fun boundary(
        topLeft: Point,
        topRight: Point,
        bottomRight: Point,
        bottomLeft: Point,
    ) = DocumentBoundary(
        topLeft = topLeft,
        topRight = topRight,
        bottomLeft = bottomLeft,
        bottomRight = bottomRight,
        confidence = 1f,
    )

    private companion object {
        const val IMAGE_SIZE = 1000
        const val FOCAL = 1500.0
        const val DISTANCE = 1200.0
        const val TILT_DEGREES = 25.0
        const val TILT_Y_DEGREES = 15.0
    }
}
