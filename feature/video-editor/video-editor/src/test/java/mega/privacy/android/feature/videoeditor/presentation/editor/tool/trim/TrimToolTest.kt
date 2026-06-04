package mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.SourceState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
class TrimToolTest {

    private val durationMs = 10_000L
    private fun state(trim: TrimState = TrimState(0L, durationMs)) =
        EditorState(source = SourceState(durationMs = durationMs), trim = trim)

    @Test
    fun `test that SetRange clamps a negative start to zero`() {
        val result = TrimTool.reduce(state(), TrimAction.SetRange(startMs = -500L, endMs = 5_000L))

        assertThat(result.trim).isEqualTo(TrimState(startMs = 0L, endMs = 5_000L))
    }

    @Test
    fun `test that SetRange clamps the end to the source duration`() {
        val result = TrimTool.reduce(state(), TrimAction.SetRange(startMs = 0L, endMs = 999_999L))

        assertThat(result.trim).isEqualTo(TrimState(startMs = 0L, endMs = durationMs))
    }

    @Test
    fun `test that SetRange keeps the end at least the start`() {
        val result = TrimTool.reduce(state(), TrimAction.SetRange(startMs = 8_000L, endMs = 2_000L))

        assertThat(result.trim).isEqualTo(TrimState(startMs = 8_000L, endMs = 8_000L))
    }

    @Test
    fun `test that SeekTo updates the playhead`() {
        val result = TrimTool.reduce(state(), TrimAction.SeekTo(ms = 3_000L))

        assertThat(result.playback.playheadMs).isEqualTo(3_000L)
    }

    @Test
    fun `test that reset restores the full range`() {
        val result = TrimTool.reset(state(trim = TrimState(2_000L, 5_000L)))

        assertThat(result.trim).isEqualTo(TrimState(startMs = 0L, endMs = durationMs))
    }

    @Test
    fun `test that isApplied is false for the full range`() {
        assertThat(TrimTool.isApplied(state(trim = TrimState(0L, durationMs)))).isFalse()
    }

    @Test
    fun `test that isApplied is true for a partial range`() {
        assertThat(TrimTool.isApplied(state(trim = TrimState(0L, 5_000L)))).isTrue()
    }

    @Test
    fun `test that captureRollback restores the trim slice`() {
        val original = state(trim = TrimState(1_000L, 5_000L))
        val rollback = TrimTool.captureRollback(original)

        val edited = TrimTool.reduce(original, TrimAction.SetRange(2_000L, 8_000L))
        val restored = rollback.restore(edited)

        assertThat(restored.trim).isEqualTo(TrimState(1_000L, 5_000L))
    }

    @Test
    fun `test that reduce ignores a non-trim action`() {
        val original = state(trim = TrimState(1_000L, 5_000L))

        val result = TrimTool.reduce(original, OtherAction)

        assertThat(result).isSameInstanceAs(original)
    }

    private object OtherAction : ToolAction
}
