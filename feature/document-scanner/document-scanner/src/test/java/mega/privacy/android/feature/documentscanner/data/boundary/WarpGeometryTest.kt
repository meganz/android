package mega.privacy.android.feature.documentscanner.data.boundary

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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

        val (width, height) = quad.warpTargetSize()

        assertThat(width).isEqualTo(640)
        assertThat(height).isEqualTo(480)
    }

    @Test
    fun `test that warpTargetSize takes the longer of each opposing edge pair`() {
        // Top edge shorter than bottom edge; left edge shorter than right edge.
        val quad = PixelQuad(
            topLeft = Point(0f, 0f),
            topRight = Point(100f, 0f), // top edge = 100
            bottomRight = Point(300f, 150f), // right edge = hypot(200, 150) = 250
            bottomLeft = Point(0f, 150f), // bottom edge = 300; left edge = 150
        )

        val (width, height) = quad.warpTargetSize()

        assertThat(width).isEqualTo(300) // max(100, 300)
        assertThat(height).isEqualTo(250) // max(150, 250)
    }

    @Test
    fun `test that warpTargetSize clamps a degenerate quad to at least one pixel`() {
        val quad = PixelQuad(
            topLeft = Point(50f, 50f),
            topRight = Point(50f, 50f),
            bottomRight = Point(50f, 50f),
            bottomLeft = Point(50f, 50f),
        )

        val (width, height) = quad.warpTargetSize()

        assertThat(width).isEqualTo(1)
        assertThat(height).isEqualTo(1)
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
}
