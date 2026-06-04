package mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim

import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction

sealed interface TrimAction : ToolAction {
    data class SetRange(val startMs: Long, val endMs: Long) : TrimAction

    /** Move the playhead to `ms`. Driven by a drag on the filmstrip's playhead. */
    data class SeekTo(val ms: Long) : TrimAction
}
