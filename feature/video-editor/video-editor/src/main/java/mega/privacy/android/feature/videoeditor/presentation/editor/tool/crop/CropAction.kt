package mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop

import android.graphics.RectF
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction

sealed interface CropAction : ToolAction {
    data class SetRect(val rect: RectF) : CropAction
    data class SetPreset(val preset: CropPreset) : CropAction
}
