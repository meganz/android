package mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
class RotateToolTest {

    private fun state(rotate: RotateState = RotateState()) = EditorState(rotate = rotate)

    @Test
    fun `test that RotateLeft subtracts ninety degrees`() {
        val result = RotateTool.reduce(state(), RotateAction.RotateLeft)

        assertThat(result.rotate.degrees).isEqualTo(-90)
    }

    @Test
    fun `test that RotateRight adds ninety degrees`() {
        val result = RotateTool.reduce(state(), RotateAction.RotateRight)

        assertThat(result.rotate.degrees).isEqualTo(90)
    }

    @Test
    fun `test that rotations accumulate as a running total`() {
        var result = state()
        repeat(3) { result = RotateTool.reduce(result, RotateAction.RotateRight) }

        assertThat(result.rotate.degrees).isEqualTo(270)
    }

    @Test
    fun `test that ToggleFlipHorizontal flips the flag`() {
        val result = RotateTool.reduce(state(), RotateAction.ToggleFlipHorizontal)

        assertThat(result.rotate.flipHorizontal).isTrue()
    }

    @Test
    fun `test that isApplied is false for the identity transform`() {
        assertThat(
            RotateTool.isApplied(
                state(
                    RotateState(
                        degrees = 0,
                        flipHorizontal = false
                    )
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that isApplied is false after a full rotation`() {
        assertThat(RotateTool.isApplied(state(RotateState(degrees = 360)))).isFalse()
    }

    @Test
    fun `test that isApplied is true when rotated`() {
        assertThat(RotateTool.isApplied(state(RotateState(degrees = 90)))).isTrue()
    }

    @Test
    fun `test that isApplied is true when flipped`() {
        assertThat(RotateTool.isApplied(state(RotateState(flipHorizontal = true)))).isTrue()
    }

    @Test
    fun `test that reset restores the identity transform`() {
        val result = RotateTool.reset(state(RotateState(degrees = 180, flipHorizontal = true)))

        assertThat(result.rotate).isEqualTo(RotateState())
    }

    @Test
    fun `test that videoEffects is empty for the identity transform`() {
        assertThat(RotateTool.videoEffects(state(RotateState()))).isEmpty()
    }

    @Test
    fun `test that videoEffects contributes one effect when rotated`() {
        assertThat(RotateTool.videoEffects(state(RotateState(degrees = 90)))).hasSize(1)
    }

    @Test
    fun `test that videoEffects negates clockwise degrees for the counterclockwise transformation`() {
        // State +90 = clockwise (preview rotationZ); Media3 is counterclockwise,
        // so the effect must carry -90, normalised by the builder to 270.
        val effect = RotateTool.videoEffects(state(RotateState(degrees = 90)))
            .single() as ScaleAndRotateTransformation

        assertThat(effect.rotationDegrees).isEqualTo(270f)
    }

    @Test
    fun `test that videoEffects maps a counterclockwise quarter turn to ninety degrees`() {
        val effect = RotateTool.videoEffects(state(RotateState(degrees = -90)))
            .single() as ScaleAndRotateTransformation

        assertThat(effect.rotationDegrees).isEqualTo(90f)
    }

    @Test
    fun `test that captureRollback restores the rotate slice`() {
        val original = state(RotateState(degrees = 90, flipHorizontal = true))
        val rollback = RotateTool.captureRollback(original)

        val edited = RotateTool.reduce(original, RotateAction.RotateRight)
        val restored = rollback.restore(edited)

        assertThat(restored.rotate).isEqualTo(RotateState(degrees = 90, flipHorizontal = true))
    }

    @Test
    fun `test that reduce ignores a non-rotate action`() {
        val original = state(RotateState(degrees = 90))

        val result = RotateTool.reduce(original, OtherAction)

        assertThat(result).isSameInstanceAs(original)
    }

    private object OtherAction : ToolAction
}
