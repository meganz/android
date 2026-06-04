package mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.feature.videoeditor.components.Filmstrip
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId
import kotlin.time.Duration.Companion.milliseconds

/**
 * Built-in Trim tool. Clamps the user-supplied range to the source duration and
 * keeps the range non-inverted; the playhead is clamped centrally by the
 * top-level reducer.
 *
 * The trim is realised at export via [androidx.media3.common.MediaItem.ClippingConfiguration]
 * on the built `MediaItem`, not as a video effect — [videoEffects] therefore
 * stays empty.
 */
@UnstableApi
object TrimTool : EditorTool {

    override val id: ToolId = BuiltInToolIds.Trim
    override val icon: ImageVector = Icons.Filled.ContentCut
    override val label: String = "Trim"

    override fun reduce(state: EditorState, action: ToolAction): EditorState {
        val trimAction = action as? TrimAction ?: return state
        return when (trimAction) {
            is TrimAction.SetRange -> {
                val start = trimAction.startMs.coerceAtLeast(0L)
                // Keep end ≥ start so the range is never inverted; an inverted
                // range would make the top-level playhead clamp throw on
                // coerceIn(start, end).
                val end = trimAction.endMs.coerceAtMost(state.source.durationMs).coerceAtLeast(start)
                state.copy(trim = state.trim.copy(startMs = start, endMs = end))
            }

            is TrimAction.SeekTo -> state.copy(
                playback = state.playback.copy(playheadMs = trimAction.ms),
            )
        }
    }

    override fun reset(state: EditorState): EditorState =
        state.copy(trim = TrimState(startMs = 0L, endMs = state.source.durationMs))

    override fun captureRollback(state: EditorState): ToolRollback {
        val saved = state.trim
        return ToolRollback { it.copy(trim = saved) }
    }

    override fun isApplied(state: EditorState): Boolean =
        !state.trim.isFullRange(state.source.durationMs)

    @Composable
    override fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    ) {
        val durationInSecondsTextMapper = remember { DurationInSecondsTextMapper() }
        val selectionMs = (state.trim.endMs - state.trim.startMs).coerceAtLeast(0L)
        // System-gesture exclusion is applied at the ToolDeck level, so the
        // filmstrip handles inherit it without re-applying here.
        Column(
            modifier = modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Filmstrip(
                sourceUri = state.source.uri,
                durationMs = state.source.durationMs,
                trimStartMs = state.trim.startMs,
                trimEndMs = state.trim.endMs,
                playheadMs = state.playback.playheadMs,
                onTrimChange = { start, end -> onAction(TrimAction.SetRange(start, end)) },
                onSeek = { ms -> onAction(TrimAction.SeekTo(ms)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MegaText(
                    text = "Selection",
                    style = AppTheme.typography.labelMedium,
                    textColor = TextColor.Secondary,
                )
                MegaText(
                    text = durationInSecondsTextMapper(selectionMs.milliseconds),
                    style = AppTheme.typography.titleSmall,
                    textColor = TextColor.Primary,
                )
            }
        }
    }
}
