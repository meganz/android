package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import mega.privacy.android.app.utils.CloudStorageOptionControlUtil

interface ToolbarActionsModifier {

    fun canHandle(item: ToolbarActionsModifierItem): Boolean

    fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    )
}

sealed interface ToolbarActionsModifierItem {

    data object CloudDriveSyncs : ToolbarActionsModifierItem
}
