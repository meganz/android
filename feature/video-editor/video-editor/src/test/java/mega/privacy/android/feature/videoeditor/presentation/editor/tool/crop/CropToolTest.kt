package mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop

import android.graphics.RectF
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.SourceState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q], manifest = Config.NONE)
class CropToolTest {

    private val srcW = 1920
    private val srcH = 1080
    private fun state(crop: CropState = CropState()) = EditorState(
        source = SourceState(durationMs = 1_000L, widthPx = srcW, heightPx = srcH),
        crop = crop,
    )

    @Test
    fun `test that SetRect copies the supplied rect into the crop slice`() {
        val result = CropTool.reduce(state(), CropAction.SetRect(RectF(0.25f, 0.25f, 0.75f, 0.75f)))

        with(result.crop.rect) {
            assertThat(left).isEqualTo(0.25f)
            assertThat(top).isEqualTo(0.25f)
            assertThat(right).isEqualTo(0.75f)
            assertThat(bottom).isEqualTo(0.75f)
        }
    }

    @Test
    fun `test that SetPreset square locks the aspect and records the preset`() {
        val result = CropTool.reduce(state(), CropAction.SetPreset(CropPreset.SQUARE))

        assertThat(result.crop.selectedPreset).isEqualTo(CropPreset.SQUARE)
        assertThat(result.crop.aspectLock).isEqualTo(1f)
        assertThat(result.crop.freeForm).isTrue()
        assertThat(result.crop.isFullFrame).isFalse()
    }

    @Test
    fun `test that SetPreset original locks to the source aspect ratio`() {
        val result = CropTool.reduce(state(), CropAction.SetPreset(CropPreset.ORIGINAL))

        assertThat(result.crop.selectedPreset).isEqualTo(CropPreset.ORIGINAL)
        assertThat(result.crop.aspectLock).isWithin(0.0001f).of(srcW.toFloat() / srcH.toFloat())
    }

    @Test
    fun `test that SetPreset free clears the aspect lock`() {
        val result = CropTool.reduce(state(), CropAction.SetPreset(CropPreset.FREE))

        assertThat(result.crop.aspectLock).isNull()
    }

    @Test
    fun `test that isApplied is false for a full-frame crop`() {
        assertThat(CropTool.isApplied(state(CropState()))).isFalse()
    }

    @Test
    fun `test that isApplied is true for a partial crop`() {
        assertThat(CropTool.isApplied(state(CropState(rect = RectF(0.1f, 0.1f, 0.9f, 0.9f))))).isTrue()
    }

    @Test
    fun `test that reset restores a full-frame crop`() {
        val result = CropTool.reset(state(CropState(rect = RectF(0.2f, 0.2f, 0.8f, 0.8f))))

        assertThat(result.crop.isFullFrame).isTrue()
    }

    @Test
    fun `test that videoEffects is empty for a full-frame crop`() {
        assertThat(CropTool.videoEffects(state(CropState()))).isEmpty()
    }

    @Test
    fun `test that videoEffects contributes one crop effect for a partial crop`() {
        val effects = CropTool.videoEffects(state(CropState(rect = RectF(0.25f, 0.25f, 0.75f, 0.75f))))

        assertThat(effects).hasSize(1)
    }

    @Test
    fun `test that captureRollback restores the crop slice`() {
        val original = state(CropState(rect = RectF(0.2f, 0.2f, 0.8f, 0.8f)))
        val rollback = CropTool.captureRollback(original)

        val edited = CropTool.reduce(original, CropAction.SetRect(RectF(0f, 0f, 1f, 1f)))
        val restored = rollback.restore(edited)

        with(restored.crop.rect) {
            assertThat(left).isEqualTo(0.2f)
            assertThat(right).isEqualTo(0.8f)
        }
    }

    @Test
    fun `test that reduce ignores a non-crop action`() {
        val original = state(CropState(rect = RectF(0.2f, 0.2f, 0.8f, 0.8f)))

        val result = CropTool.reduce(original, OtherAction)

        assertThat(result).isSameInstanceAs(original)
    }

    private object OtherAction : ToolAction
}
