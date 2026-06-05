package mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.videoeditor.components.MAX_VOLUME
import mega.privacy.android.feature.videoeditor.components.VolumeSlider
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Built-in Volume tool. Audio gain is realised at export via a
 * [ChannelMixingAudioProcessor] whose identity matrix is scaled by `volume`.
 *
 * The preview player has its own volume property (capped at 1f); the processor
 * here is only used at export, where amplification > 1 is valid.
 */
@UnstableApi
object VolumeTool : EditorTool {

    override val id: ToolId = BuiltInToolIds.Volume
    override val icon: ImageVector = Icons.AutoMirrored.Filled.VolumeUp
    override val label: String = "Volume"

    override fun reduce(state: EditorState, action: ToolAction): EditorState {
        val volumeAction = action as? VolumeAction ?: return state
        return when (volumeAction) {
            is VolumeAction.SetVolume ->
                state.copy(volume = VolumeState(volumeAction.volume.coerceIn(0f, MAX_VOLUME)))
        }
    }

    override fun reset(state: EditorState): EditorState =
        state.copy(volume = VolumeState())

    override fun captureRollback(state: EditorState): ToolRollback {
        val saved = state.volume
        return ToolRollback { it.copy(volume = saved) }
    }

    override fun isApplied(state: EditorState): Boolean =
        !state.volume.isIdentity

    override fun audioProcessors(state: EditorState): List<AudioProcessor> {
        if (state.volume.isIdentity) return emptyList()
        val gain = state.volume.volume
        val processor = ChannelMixingAudioProcessor()
        for (channelCount in intArrayOf(1, 2, 4, 6, 8)) {
            processor.putChannelMixingMatrix(scaledIdentityMatrix(channelCount, gain))
        }
        return listOf(processor)
    }

    @Composable
    override fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    ) {
        val percent = (state.volume.volume * 100).toInt()
        val label = if (percent == 0) "Mute" else "$percent%"
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        // From mute → 100% (restore passthrough); from any other
                        // volume → mute. A two-step toggle.
                        .clickable { onAction(VolumeAction.SetVolume(if (percent == 0) 1f else 0f)) },
                    contentAlignment = Alignment.Center,
                ) {
                    MegaIcon(
                        imageVector = if (percent == 0) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        tint = IconColor.Primary,
                        contentDescription = if (percent == 0) "Unmute" else "Mute",
                    )
                }
                VolumeSlider(
                    value = state.volume.volume,
                    onValueChange = { onAction(VolumeAction.SetVolume(it)) },
                    modifier = Modifier.weight(1f),
                )
                MegaText(
                    text = label,
                    style = AppTheme.typography.titleSmall,
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(48.dp),
                )
            }
            // Soft caveat, only when the user pushes past unity gain.
            if (state.volume.volume > 1f) {
                MegaText(
                    text = "Boost above 100% applies on export only.",
                    style = AppTheme.typography.bodySmall,
                    textColor = TextColor.Secondary,
                )
            }
        }
    }

    private fun scaledIdentityMatrix(channelCount: Int, scale: Float): ChannelMixingMatrix {
        val coefficients = FloatArray(channelCount * channelCount)
        for (channel in 0 until channelCount) {
            coefficients[channel * channelCount + channel] = scale
        }
        return ChannelMixingMatrix(channelCount, channelCount, coefficients)
    }
}
