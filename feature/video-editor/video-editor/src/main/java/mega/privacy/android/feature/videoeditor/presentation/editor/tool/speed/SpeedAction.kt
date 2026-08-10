package mega.privacy.android.feature.videoeditor.presentation.editor.tool.speed

import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction

sealed interface SpeedAction : ToolAction {
    data class SetSpeed(val speed: Float) : SpeedAction
}
