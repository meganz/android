package mega.privacy.android.feature.videoeditor.presentation.editor.tool.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback

/**
 * Pluggable contract for an editing tool.
 *
 * A tool owns:
 * - a slice of [EditorState] (read it inside its reducer; the slice lives on
 *   [EditorState] directly for built-ins, or in [EditorState.extras] — via
 *   [mega.privacy.android.feature.videoeditor.presentation.editor.state.toolState] /
 *   [mega.privacy.android.feature.videoeditor.presentation.editor.state.withToolState] — for custom tools that
 *   need scratch state)
 * - a [reduce] function applied when the editor receives a [ToolAction] of
 *   the tool's expected subtype
 * - a [captureRollback] that snapshots its slice for Cancel
 * - the bottom-panel UI shown while the tool is active
 * - an optional preview overlay drawn on top of the player
 * - the Media3 effects and audio processors it contributes to export
 * - lifecycle hints for whether to auto-pause on entry / auto-resume on Apply
 *
 * Built-in tools live in `mega.privacy.android.feature.videoeditor.presentation.editor.tool.{trim,crop,…}` and
 * are registered into the
 * [mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry] via Hilt multibindings.
 */
@UnstableApi
interface EditorTool {
    /** Stable identifier; used as map key in [mega.privacy.android.feature.videoeditor.presentation.editor.engine.ToolRegistry]. */
    val id: ToolId

    /** Tab-row icon. */
    val icon: ImageVector

    /** Display label (also used as fallback toolbar title). */
    val label: String

    /**
     * Apply a tool-specific action to the state. Tools should `as?`-narrow the
     * action to their own action type and return the state unchanged on a
     * mismatch — the dispatcher routes only this tool's actions here, but
     * `as?` is the defensive read.
     */
    fun reduce(state: EditorState, action: ToolAction): EditorState

    /** Reset the tool's slice to defaults (called by the "Reset" affordance). */
    fun reset(state: EditorState): EditorState

    /**
     * Capture a rollback for this tool's current state (used by Cancel). Read
     * the slice you own *now* into a local, then return a [ToolRollback] that
     * writes it back:
     *
     * ```
     * val saved = state.crop
     * return ToolRollback { it.copy(crop = saved) }
     * ```
     *
     * The captured value stays inside the closure, so its type never leaves the
     * tool — no casting at the call site. Default: [ToolRollback.None] for tools
     * that have nothing to roll back. Custom tools restore via
     * [mega.privacy.android.feature.videoeditor.presentation.editor.state.withToolState].
     */
    fun captureRollback(state: EditorState): ToolRollback = ToolRollback.None

    /**
     * Whether entering this tool should auto-pause the player.
     *
     * Crop sets this to `false` because on long high-bitrate videos the video
     * decoder discards reference frames during pause; pausing-then-resuming on
     * Apply then takes several seconds to re-establish them. Other tools
     * (Trim especially) benefit from a paused frame for precise scrubbing.
     */
    val pauseOnEnter: Boolean get() = true

    /** Whether Apply should auto-resume playback. Mirror of [pauseOnEnter]. */
    val resumeOnApply: Boolean get() = pauseOnEnter

    /**
     * Whether this tool currently contributes a non-identity change to the
     * output. Drives the "applied" badge shown on the tab bar so the user can
     * see at a glance which tools have committed edits. Default `false`.
     */
    fun isApplied(state: EditorState): Boolean = false

    /** Media3 video effects this tool contributes at export. */
    fun videoEffects(state: EditorState): List<Effect> = emptyList()

    /** Media3 audio processors this tool contributes at export. */
    fun audioProcessors(state: EditorState): List<AudioProcessor> = emptyList()

    /**
     * Bottom-panel composable shown while the tool is active.
     *
     * Implementations dispatch their own [ToolAction]s through `onAction`;
     * the view-model wraps them in
     * [mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction.DispatchTool].
     */
    @Composable
    fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    )

    /**
     * Optional overlay rendered above the preview surface while this tool is
     * active. Default: nothing.
     *
     * `previewSize` is the canvas-fit srcAspect rect the AndroidView occupies
     * inside the preview container (in pixels), so the overlay can map
     * normalised source coordinates onto screen positions.
     */
    @Composable
    fun PreviewOverlay(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        previewSize: IntSize,
        modifier: Modifier,
    ) {
        // default: no overlay
    }
}
