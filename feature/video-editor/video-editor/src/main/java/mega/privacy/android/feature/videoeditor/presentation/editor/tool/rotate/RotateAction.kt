package mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate

import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction

sealed interface RotateAction : ToolAction {
    data object RotateLeft : RotateAction
    data object RotateRight : RotateAction
    data object ToggleFlipHorizontal : RotateAction
}
