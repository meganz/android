package mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
class SpeedToolTest {

    private fun state(speed: SpeedState = SpeedState()) = EditorState(speed = speed)

    @Test
    fun `test that SetSpeed sets the requested speed`() {
        val result = SpeedTool.reduce(state(), SpeedAction.SetSpeed(2f))

        assertThat(result.speed.speed).isEqualTo(2f)
    }

    @Test
    fun `test that SetSpeed clamps below the minimum`() {
        val result = SpeedTool.reduce(state(), SpeedAction.SetSpeed(0.01f))

        assertThat(result.speed.speed).isEqualTo(0.1f)
    }

    @Test
    fun `test that SetSpeed clamps above the maximum`() {
        val result = SpeedTool.reduce(state(), SpeedAction.SetSpeed(50f))

        assertThat(result.speed.speed).isEqualTo(10f)
    }

    @Test
    fun `test that SetSpeed ignores a non-finite speed`() {
        val original = state(SpeedState(2f))

        assertThat(SpeedTool.reduce(original, SpeedAction.SetSpeed(Float.NaN))).isSameInstanceAs(original)
        assertThat(SpeedTool.reduce(original, SpeedAction.SetSpeed(Float.POSITIVE_INFINITY)))
            .isSameInstanceAs(original)
    }

    @Test
    fun `test that SetSpeed ignores a non-positive speed`() {
        val original = state(SpeedState(2f))

        assertThat(SpeedTool.reduce(original, SpeedAction.SetSpeed(0f))).isSameInstanceAs(original)
        assertThat(SpeedTool.reduce(original, SpeedAction.SetSpeed(-1f))).isSameInstanceAs(original)
    }

    @Test
    fun `test that isApplied is false at real-time speed`() {
        assertThat(SpeedTool.isApplied(state(SpeedState(1f)))).isFalse()
    }

    @Test
    fun `test that isApplied is true at a non-default speed`() {
        assertThat(SpeedTool.isApplied(state(SpeedState(2f)))).isTrue()
    }

    @Test
    fun `test that reset restores real-time speed`() {
        val result = SpeedTool.reset(state(SpeedState(2f)))

        assertThat(result.speed).isEqualTo(SpeedState())
    }

    @Test
    fun `test that videoEffects is empty at real-time speed`() {
        assertThat(SpeedTool.videoEffects(state(SpeedState(1f)))).isEmpty()
    }

    @Test
    fun `test that videoEffects contributes one effect at a non-default speed`() {
        assertThat(SpeedTool.videoEffects(state(SpeedState(2f)))).hasSize(1)
    }

    @Test
    fun `test that audioProcessors is empty at real-time speed`() {
        assertThat(SpeedTool.audioProcessors(state(SpeedState(1f)))).isEmpty()
    }

    @Test
    fun `test that audioProcessors contributes one processor at a non-default speed`() {
        assertThat(SpeedTool.audioProcessors(state(SpeedState(2f)))).hasSize(1)
    }

    @Test
    fun `test that captureRollback restores the speed slice`() {
        val original = state(SpeedState(2f))
        val rollback = SpeedTool.captureRollback(original)

        val edited = SpeedTool.reduce(original, SpeedAction.SetSpeed(4f))
        val restored = rollback.restore(edited)

        assertThat(restored.speed).isEqualTo(SpeedState(2f))
    }

    @Test
    fun `test that reduce ignores a non-speed action`() {
        val original = state(SpeedState(2f))

        val result = SpeedTool.reduce(original, OtherAction)

        assertThat(result).isSameInstanceAs(original)
    }

    private object OtherAction : ToolAction
}
