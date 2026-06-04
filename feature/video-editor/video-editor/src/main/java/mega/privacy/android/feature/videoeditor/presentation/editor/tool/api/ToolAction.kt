package mega.privacy.android.feature.videoeditor.presentation.editor.tool.api

/**
 * Marker interface for tool-specific actions. Each tool defines its own sealed
 * hierarchy implementing this; the editor view-model routes [ToolAction]s to
 * the active tool's reducer via
 * [mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorAction.DispatchTool].
 */
interface ToolAction
