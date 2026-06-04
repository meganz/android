package mega.privacy.android.feature.videoeditor.presentation.editor.state

import androidx.compose.runtime.Immutable
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop.CropState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate.RotateState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed.SpeedState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.trim.TrimState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume.VolumeState

/**
 * Top-level editor state. One immutable value flows through the unidirectional
 * data-flow loop: actions dispatched on the
 * [mega.privacy.android.feature.videoeditor.presentation.editor.EditorViewModel]
 * reduce this state, the new value re-renders the UI.
 *
 * Each tool owns its own slice. Built-ins use the named slices below; custom
 * tools that need scratch state keep it in [extras], keyed by their
 * [ToolId] (see [toolState] / [withToolState]). The view-model never reaches
 * inside a slice directly — it dispatches a tool action and the tool's reducer
 * produces the new slice. `toolSnapshot` holds the per-tool values captured
 * when a tool was entered so Cancel can roll back.
 */
@Immutable
data class EditorState(
    val source: SourceState = SourceState(),
    val playback: PlaybackState = PlaybackState(),
    val trim: TrimState = TrimState(),
    val crop: CropState = CropState(),
    val rotate: RotateState = RotateState(),
    val speed: SpeedState = SpeedState(),
    val volume: VolumeState = VolumeState(),
    val activeTool: ToolId? = null,
    val toolSnapshot: ToolSnapshot? = null,
    // Scratch state for custom tools that don't have a named slice above. Values
    // are opaque to the core and MUST be immutable (the @Immutable contract on
    // EditorState extends to them). Built-in tools do not use this map.
    val extras: Map<ToolId, Any> = emptyMap(),
)

/** This tool's entry in [EditorState.extras], or null if it has none. */
fun EditorState.toolState(id: ToolId): Any? = extras[id]

/**
 * Set (or, with `null`, clear) this tool's entry in [EditorState.extras].
 * Custom tools call this from their reducer to persist scratch state, and from
 * the [ToolRollback] they return in
 * [mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool.captureRollback] to roll it
 * back — e.g. `val saved = toolState(id); ToolRollback { it.withToolState(id, saved) }`.
 */
fun EditorState.withToolState(id: ToolId, value: Any?): EditorState =
    copy(extras = if (value == null) extras - id else extras + (id to value))

/**
 * A type-safe, one-shot rollback for a single tool. A tool produces one in
 * [mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool.captureRollback] by closing
 * over its own (statically-typed) slice; [restore] applies that captured slice
 * back onto a later state. Because the captured value lives inside the closure,
 * its type never escapes to the framework — no `Any`, no casts, and it is
 * impossible to feed one tool's captured value to another tool's restore.
 */
fun interface ToolRollback {
    fun restore(state: EditorState): EditorState

    companion object {
        /** No-op rollback for tools that have no state to restore. */
        val None: ToolRollback = ToolRollback { it }
    }
}

/**
 * Pre-edit snapshot captured on enterTool so Cancel can restore the exact state
 * the user entered the tool with — regardless of which tool's slice was
 * actually mutated. Holds one [ToolRollback] per registered tool; [restore]
 * folds them onto the cancel-time state.
 */
@Immutable
class ToolSnapshot(private val rollbacks: List<ToolRollback>) {
    fun restore(state: EditorState): EditorState =
        rollbacks.fold(state) { acc, rollback -> rollback.restore(acc) }
}
