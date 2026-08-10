package mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
class VolumeToolTest {

    private fun state(volume: VolumeState = VolumeState()) = EditorState(volume = volume)

    @Test
    fun `test that SetVolume sets the requested gain`() {
        val result = VolumeTool.reduce(state(), VolumeAction.SetVolume(1.5f))

        assertThat(result.volume.volume).isEqualTo(1.5f)
    }

    @Test
    fun `test that SetVolume clamps below zero to mute`() {
        val result = VolumeTool.reduce(state(), VolumeAction.SetVolume(-1f))

        assertThat(result.volume.volume).isEqualTo(0f)
    }

    @Test
    fun `test that SetVolume clamps above the maximum boost`() {
        val result = VolumeTool.reduce(state(), VolumeAction.SetVolume(5f))

        assertThat(result.volume.volume).isEqualTo(2f)
    }

    @Test
    fun `test that isApplied is false at passthrough gain`() {
        assertThat(VolumeTool.isApplied(state(VolumeState(1f)))).isFalse()
    }

    @Test
    fun `test that isApplied is true when muted`() {
        assertThat(VolumeTool.isApplied(state(VolumeState(0f)))).isTrue()
    }

    @Test
    fun `test that isApplied is true when boosted`() {
        assertThat(VolumeTool.isApplied(state(VolumeState(1.5f)))).isTrue()
    }

    @Test
    fun `test that reset restores passthrough gain`() {
        val result = VolumeTool.reset(state(VolumeState(0f)))

        assertThat(result.volume).isEqualTo(VolumeState())
    }

    @Test
    fun `test that audioProcessors is empty at passthrough gain`() {
        assertThat(VolumeTool.audioProcessors(state(VolumeState(1f)))).isEmpty()
    }

    @Test
    fun `test that audioProcessors contributes one processor at a non-default gain`() {
        assertThat(VolumeTool.audioProcessors(state(VolumeState(0f)))).hasSize(1)
    }

    @Test
    fun `test that captureRollback restores the volume slice`() {
        val original = state(VolumeState(0.5f))
        val rollback = VolumeTool.captureRollback(original)

        val edited = VolumeTool.reduce(original, VolumeAction.SetVolume(2f))
        val restored = rollback.restore(edited)

        assertThat(restored.volume).isEqualTo(VolumeState(0.5f))
    }

    @Test
    fun `test that reduce ignores a non-volume action`() {
        val original = state(VolumeState(0.5f))

        val result = VolumeTool.reduce(original, OtherAction)

        assertThat(result).isSameInstanceAs(original)
    }

    private object OtherAction : ToolAction
}
