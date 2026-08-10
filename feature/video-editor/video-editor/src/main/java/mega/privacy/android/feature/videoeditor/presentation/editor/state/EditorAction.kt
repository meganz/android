package mega.privacy.android.feature.videoeditor.presentation.editor.state

import android.net.Uri
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Top-level editor actions, excluding tool-specific edits.
 *
 * Tool-specific edits (e.g. CropAction.SetRect) implement [ToolAction] and are
 * dispatched via [EditorAction.DispatchTool], which the view-model routes to
 * the matching tool's reducer.
 */
sealed interface EditorAction {
    data class LoadVideo(val uri: Uri) : EditorAction
    data class SourceLoaded(
        val uri: Uri,
        val durationMs: Long,
        val widthPx: Int,
        val heightPx: Int,
    ) : EditorAction

    /** The source's metadata could not be read — the video can't be edited. */
    data class SourceLoadFailed(val uri: Uri) : EditorAction
    data class SourceSizeChanged(val widthPx: Int, val heightPx: Int) : EditorAction
    data object ClearSource : EditorAction
    data class SetPlaying(val isPlaying: Boolean) : EditorAction
    data class SetPlayhead(val ms: Long) : EditorAction
    data class EnterTool(val tool: ToolId) : EditorAction
    data object CancelTool : EditorAction
    data object ApplyTool : EditorAction
    data object ResetActiveTool : EditorAction

    /** Route to the currently-active tool's reducer. */
    data class DispatchTool(val action: ToolAction) : EditorAction
}
