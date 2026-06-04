package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.SourceState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim.TrimState

/**
 * Pure reducer for top-level [EditorAction]s. Tool-specific actions are
 * routed to the active tool via [registry].
 *
 * Lifecycle:
 * - [EditorAction.EnterTool] captures a snapshot and optionally pauses
 *   (tool.pauseOnEnter).
 * - [EditorAction.ApplyTool] clears the active tool and optionally resumes
 *   (tool.resumeOnApply).
 * - [EditorAction.CancelTool] restores the snapshot.
 *
 * No side-effects live here — the view-model takes care of dispatching async
 * work (load metadata, export) after producing the new state.
 */
@UnstableApi
fun reduce(state: EditorState, action: EditorAction, registry: ToolRegistry): EditorState =
    reduceCore(state, action, registry).clampPlayheadToTrim()

/**
 * Keep `playback.playheadMs` inside `[trim.startMs, trim.endMs]` so tool
 * reducers don't have to mutate the playback slice themselves whenever they
 * touch the trim range — keeps each tool's reducer to its own slice.
 *
 * Invariant required by callers: `trim.startMs ≤ trim.endMs`. `TrimTool` and
 * `SourceLoaded` both maintain it; `LoadVideo` / `ClearSource` reset both
 * slices to zeros, so a zero-length clamp is a no-op.
 */
private fun EditorState.clampPlayheadToTrim(): EditorState {
    val clamped = playback.playheadMs.coerceIn(trim.startMs, trim.endMs)
    return if (clamped == playback.playheadMs) this
    else copy(playback = playback.copy(playheadMs = clamped))
}

@OptIn(UnstableApi::class)
private fun reduceCore(
    state: EditorState,
    action: EditorAction,
    registry: ToolRegistry,
): EditorState =
    when (action) {
        is EditorAction.LoadVideo -> EditorState(
            source = SourceState(uri = action.uri),
        )

        is EditorAction.SourceLoaded ->
            // Ignore results from a metadata read whose source is no longer the
            // active one (e.g. the user swapped videos before the read finished),
            // so a stale read can't overwrite the current source's dimensions.
            if (action.uri != state.source.uri) state
            else state.copy(
                source = state.source.copy(
                    durationMs = action.durationMs,
                    widthPx = action.widthPx,
                    heightPx = action.heightPx,
                ),
                trim = TrimState(startMs = 0L, endMs = action.durationMs),
                playback = state.playback.copy(playheadMs = 0L),
            )

        is EditorAction.SourceSizeChanged -> {
            if (action.widthPx <= 0 || action.heightPx <= 0) state
            else if (state.source.widthPx == action.widthPx &&
                state.source.heightPx == action.heightPx
            ) state
            else state.copy(
                source = state.source.copy(
                    widthPx = action.widthPx,
                    heightPx = action.heightPx,
                ),
            )
        }

        EditorAction.ClearSource -> EditorState()

        is EditorAction.SetPlaying -> state.copy(
            playback = state.playback.copy(isPlaying = action.isPlaying),
        )

        is EditorAction.SetPlayhead -> state.copy(
            playback = state.playback.copy(
                playheadMs = action.ms.coerceIn(state.trim.startMs, state.trim.endMs),
            ),
        )

        is EditorAction.EnterTool -> {
            if (state.activeTool == action.tool) state
            else {
                val tool = registry[action.tool] ?: return state
                state.copy(
                    activeTool = action.tool,
                    toolSnapshot = registry.captureSnapshot(state),
                    playback = if (tool.pauseOnEnter) {
                        state.playback.copy(isPlaying = false)
                    } else state.playback,
                )
            }
        }

        EditorAction.CancelTool ->
            // The snapshot folds each tool's rollback onto the current state — a
            // no-op when there's no snapshot (let-block short-circuits to the
            // original state). The outer copy then clears the tool.
            (state.toolSnapshot?.restore(state) ?: state)
                .copy(activeTool = null, toolSnapshot = null)

        EditorAction.ApplyTool -> {
            val tool = state.activeTool?.let(registry::get)
            state.copy(
                activeTool = null,
                toolSnapshot = null,
                playback = if (tool?.resumeOnApply == true) {
                    state.playback.copy(isPlaying = true)
                } else state.playback,
            )
        }

        EditorAction.ResetActiveTool -> {
            val tool = state.activeTool?.let(registry::get) ?: return state
            tool.reset(state)
        }

        is EditorAction.DispatchTool -> {
            val tool = state.activeTool?.let(registry::get) ?: return state
            tool.reduce(state, action.action)
        }
    }
