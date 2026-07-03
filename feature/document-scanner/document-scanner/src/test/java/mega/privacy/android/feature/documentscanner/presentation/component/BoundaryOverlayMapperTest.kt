package mega.privacy.android.feature.documentscanner.presentation.component

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoundaryOverlayMapperTest {

    private fun boundary(
        tl: Point, tr: Point, br: Point, bl: Point,
    ) = DocumentBoundary(topLeft = tl, topRight = tr, bottomRight = br, bottomLeft = bl, confidence = 1f)

    @Test
    fun `test that a same-aspect frame maps normalised corners straight to view pixels`() {
        val b = boundary(
            tl = Point(0f, 0f), tr = Point(1f, 0f),
            br = Point(1f, 1f), bl = Point(0f, 1f),
        )

        val result = BoundaryOverlayMapper.map(b, frameWidth = 100, frameHeight = 200, viewWidth = 300f, viewHeight = 600f)

        // No crop when aspect ratios match: full frame fills the full view.
        assertThat(result).containsExactly(
            Point(0f, 0f), Point(300f, 0f), Point(300f, 600f), Point(0f, 600f),
        ).inOrder()
    }

    @Test
    fun `test that a wider view crops the frame vertically and offsets y`() {
        // frame 100x100 (square) into a 200x100 view (wide): scale = max(2, 1) = 2,
        // scaledHeight = 200 so it overflows the 100-tall view, cropped by 50 each side.
        val b = boundary(
            tl = Point(0f, 0f), tr = Point(1f, 0f),
            br = Point(1f, 1f), bl = Point(0f, 1f),
        )

        val result = BoundaryOverlayMapper.map(b, frameWidth = 100, frameHeight = 100, viewWidth = 200f, viewHeight = 100f)

        assertThat(result).containsExactly(
            Point(0f, -50f), Point(200f, -50f), Point(200f, 150f), Point(0f, 150f),
        ).inOrder()
    }

    @Test
    fun `test that the centre point stays centred regardless of crop`() {
        val b = boundary(
            tl = Point(0.5f, 0.5f), tr = Point(0.5f, 0.5f),
            br = Point(0.5f, 0.5f), bl = Point(0.5f, 0.5f),
        )

        val result = BoundaryOverlayMapper.map(b, frameWidth = 100, frameHeight = 100, viewWidth = 200f, viewHeight = 400f)

        result.forEach {
            assertThat(it.x).isWithin(0.001f).of(100f)
            assertThat(it.y).isWithin(0.001f).of(200f)
        }
    }

    @Test
    fun `test that non-positive dimensions return an empty list`() {
        val b = boundary(
            tl = Point(0f, 0f), tr = Point(1f, 0f),
            br = Point(1f, 1f), bl = Point(0f, 1f),
        )

        assertThat(BoundaryOverlayMapper.map(b, 0, 100, 200f, 200f)).isEmpty()
        assertThat(BoundaryOverlayMapper.map(b, 100, 100, 0f, 200f)).isEmpty()
    }
}
