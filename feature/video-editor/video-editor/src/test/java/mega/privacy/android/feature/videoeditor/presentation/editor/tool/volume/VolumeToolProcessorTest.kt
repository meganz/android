package mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume

import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric because [androidx.media3.common.audio.ChannelMixingAudioProcessor]
 * stores its matrices in an [android.util.SparseArray], which is a no-op stub on a plain JVM.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q], manifest = Config.NONE)
class VolumeToolProcessorTest {

    @Test
    fun `test that the gain processor accepts every channel count up to eight`() {
        // The processor throws UnhandledAudioFormatException for any decoded
        // channel count without a registered matrix — including the uncommon
        // layouts (3.0, 5.0, 6.1) — which would fail the whole export.
        val state = EditorState(volume = VolumeState(0.5f))
        val processor = VolumeTool.audioProcessors(state).single()

        for (channelCount in 1..8) {
            val output = processor.configure(
                AudioProcessor.AudioFormat(44_100, channelCount, C.ENCODING_PCM_16BIT),
            )
            assertThat(output.channelCount).isEqualTo(channelCount)
        }
    }
}
