package mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SpeedChangeEffect
import mega.privacy.android.feature.videoeditor.components.SpeedChip
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Built-in Speed tool.
 *
 * Contributes a Media3 [SpeedChangeEffect] for video and a [SonicAudioProcessor]
 * to keep audio in sync (pitch-corrected). The preview applies speed via
 * `ExoPlayer.setPlaybackSpeed`, so this tool only owns the reducer, panel and
 * export effects.
 */
@UnstableApi
object SpeedTool : EditorTool {

    override val id: ToolId = BuiltInToolIds.Speed
    override val icon: ImageVector = Icons.Filled.Speed
    override val label: String = "Speed"

    private const val MIN_SPEED = 0.1f
    private const val MAX_SPEED = 10f

    override fun reduce(state: EditorState, action: ToolAction): EditorState {
        val speedAction = action as? SpeedAction ?: return state
        return when (speedAction) {
            is SpeedAction.SetSpeed -> {
                // Reject non-finite / non-positive rates: they propagate to
                // ExoPlayer.setPlaybackSpeed (which throws on speed ≤ 0) and to
                // the export SpeedChangeEffect. Clamp to a sane playback range.
                if (!speedAction.speed.isFinite() || speedAction.speed <= 0f) return state
                state.copy(speed = SpeedState(speedAction.speed.coerceIn(MIN_SPEED, MAX_SPEED)))
            }
        }
    }

    override fun reset(state: EditorState): EditorState =
        state.copy(speed = SpeedState())

    override fun captureRollback(state: EditorState): ToolRollback {
        val saved = state.speed
        return ToolRollback { it.copy(speed = saved) }
    }

    override fun isApplied(state: EditorState): Boolean =
        !state.speed.isIdentity

    override fun videoEffects(state: EditorState): List<Effect> {
        if (state.speed.isIdentity) return emptyList()
        return listOf(SpeedChangeEffect(state.speed.speed))
    }

    override fun audioProcessors(state: EditorState): List<AudioProcessor> {
        if (state.speed.isIdentity) return emptyList()
        return listOf(SonicAudioProcessor().apply { setSpeed(state.speed.speed) })
    }

    @Composable
    override fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    ) {
        // Just the chip row — the selected chip reads the speed; the new clip
        // duration can be verified by playing the preview.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpeedOptions.forEach { speed ->
                SpeedChip(
                    label = "${if (speed % 1f == 0f) speed.toInt() else speed}×",
                    selected = state.speed.speed == speed,
                    onClick = { onAction(SpeedAction.SetSpeed(speed)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
