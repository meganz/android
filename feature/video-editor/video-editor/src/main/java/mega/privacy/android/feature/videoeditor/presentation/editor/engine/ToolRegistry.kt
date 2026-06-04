package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import androidx.compose.runtime.Stable
import androidx.media3.common.util.UnstableApi
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolSnapshot
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Ordered list of available tools. Order = tab-bar display order.
 *
 * The set of tools is supplied by Hilt: each built-in tool contributes itself
 * via an `@IntoSet` binding (see the feature's DI module), and the registry is
 * provided from that set. Tab-bar order therefore follows registration order.
 */
// @Stable: the registry's externally-observable state (`tools`, `byId`) is set
// in the constructor and never changes — Compose can skip composables that
// receive a ToolRegistry parameter when the reference hasn't changed.
@Stable
@UnstableApi
class ToolRegistry(tools: List<EditorTool>) {
    // Normalise duplicate IDs to a single tool (last instance wins) so a caller
    // can replace a built-in by contributing a same-ID tool. Without this both
    // the tab bar and the effect composer — which iterate `tools` directly —
    // would show two tabs and run both tools' effects at export. LinkedHashMap
    // keeps each ID at its first-seen position, so a replacement keeps the
    // original tab order.
    private val byId: Map<ToolId, EditorTool> =
        LinkedHashMap<ToolId, EditorTool>().apply {
            tools.forEach { put(it.id, it) }
        }

    /** Registered tools, de-duplicated by ID. Order = tab-bar display order. */
    val tools: List<EditorTool> = byId.values.toList()

    operator fun get(id: ToolId): EditorTool? = byId[id]

    fun require(id: ToolId): EditorTool =
        byId[id] ?: error("Tool not registered: $id")

    /**
     * Capture a [ToolSnapshot] for Cancel — one
     * [mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback] per registered tool.
     * Captures *all* tools, not just the active one, so Cancel can undo a slice
     * even if some tool mutated one it doesn't nominally own. The snapshot
     * restores itself via [ToolSnapshot.restore].
     */
    fun captureSnapshot(state: EditorState): ToolSnapshot =
        ToolSnapshot(tools.map { it.captureRollback(state) })
}
