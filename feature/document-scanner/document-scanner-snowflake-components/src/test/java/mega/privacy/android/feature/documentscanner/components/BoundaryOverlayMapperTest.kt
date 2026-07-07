package mega.privacy.android.feature.documentscanner.components

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoundaryOverlayMapperTest {

    private val unitSquare = listOf(
        Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f),
    )

    @Test
    fun `test that a same-aspect frame maps normalised corners straight to view pixels`() {
        val result = BoundaryOverlayMapper.map(unitSquare, frameWidth = 100, frameHeight = 200, viewWidth = 300f, viewHeight = 600f)

        assertThat(result).containsExactly(
            Offset(0f, 0f), Offset(300f, 0f), Offset(300f, 600f), Offset(0f, 600f),
        ).inOrder()
    }

    @Test
    fun `test that a wider view crops the frame vertically and offsets y`() {
        val result = BoundaryOverlayMapper.map(unitSquare, frameWidth = 100, frameHeight = 100, viewWidth = 200f, viewHeight = 100f)

        assertThat(result).containsExactly(
            Offset(0f, -50f), Offset(200f, -50f), Offset(200f, 150f), Offset(0f, 150f),
        ).inOrder()
    }

    @Test
    fun `test that the centre point stays centred regardless of crop`() {
        val centre = List(4) { Offset(0.5f, 0.5f) }

        val result = BoundaryOverlayMapper.map(centre, frameWidth = 100, frameHeight = 100, viewWidth = 200f, viewHeight = 400f)

        result.forEach {
            assertThat(it.x).isWithin(0.001f).of(100f)
            assertThat(it.y).isWithin(0.001f).of(200f)
        }
    }

    @Test
    fun `test that non-positive dimensions return an empty list`() {
        assertThat(BoundaryOverlayMapper.map(unitSquare, 0, 100, 200f, 200f)).isEmpty()
        assertThat(BoundaryOverlayMapper.map(unitSquare, 100, 100, 0f, 200f)).isEmpty()
    }
}
