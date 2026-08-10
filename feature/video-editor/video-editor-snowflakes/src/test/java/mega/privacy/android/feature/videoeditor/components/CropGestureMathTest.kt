package mega.privacy.android.feature.videoeditor.components

import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q], manifest = Config.NONE)
class CropGestureMathTest {

    @Test
    fun `test that videoBounds letterboxes a source wider than the canvas`() {
        val vb = videoBounds(Size(100f, 100f), sourceWidth = 200, sourceHeight = 100)

        assertThat(vb.left).isEqualTo(0f)
        assertThat(vb.top).isEqualTo(25f)
        assertThat(vb.width).isEqualTo(100f)
        assertThat(vb.height).isEqualTo(50f)
    }

    @Test
    fun `test that videoBounds pillarboxes a source taller than the canvas`() {
        val vb = videoBounds(Size(100f, 100f), sourceWidth = 100, sourceHeight = 200)

        assertThat(vb.left).isEqualTo(25f)
        assertThat(vb.top).isEqualTo(0f)
        assertThat(vb.width).isEqualTo(50f)
        assertThat(vb.height).isEqualTo(100f)
    }

    @Test
    fun `test that screenToSrc maps canvas centre to the source centre`() {
        val vb = VideoBounds(0f, 0f, 100f, 100f)

        val (sx, sy) = screenToSrc(Offset(50f, 50f), vb, Offset.Zero, 1f)

        assertThat(sx).isWithin(0.0001f).of(0.5f)
        assertThat(sy).isWithin(0.0001f).of(0.5f)
    }

    @Test
    fun `test that nearestHandle picks the closest corner within radius`() {
        val cropScreen = RectF(10f, 10f, 90f, 90f)

        assertThat(nearestHandle(Offset(12f, 12f), cropScreen, radius = 80f)).isEqualTo(CropHandle.TL)
        assertThat(nearestHandle(Offset(88f, 88f), cropScreen, radius = 80f)).isEqualTo(CropHandle.BR)
    }

    @Test
    fun `test that nearestHandle returns null when no corner is in range`() {
        val cropScreen = RectF(10f, 10f, 90f, 90f)

        assertThat(nearestHandle(Offset(50f, 50f), cropScreen, radius = 20f)).isNull()
    }

    @Test
    fun `test that resizeFree moves the dragged corner and respects the minimum side`() {
        val result = resizeFree(
            handle = CropHandle.TL,
            dragX = 0.2f,
            dragY = 0.3f,
            currentRect = RectF(0f, 0f, 1f, 1f),
            minSide = 0.1f,
        )

        assertThat(result.left).isWithin(0.0001f).of(0.2f)
        assertThat(result.top).isWithin(0.0001f).of(0.3f)
    }
}
