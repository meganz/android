package mega.privacy.android.feature.videoeditor.presentation.editor.tool.volume

import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction

sealed interface VolumeAction : ToolAction {
    data class SetVolume(val volume: Float) : VolumeAction
}
