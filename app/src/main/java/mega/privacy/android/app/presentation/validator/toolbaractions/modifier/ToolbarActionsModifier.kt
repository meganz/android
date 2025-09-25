package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.AudioToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.CloudDriveSyncsToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.DocumentSectionToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.IncomingSharesToolbarActionsModifierItem
import mega.privacy.android.app.presentation.validator.toolbaractions.model.modifier.OutgoingSharesToolbarActionsModifierItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil

interface ToolbarActionsModifier {

    fun canHandle(item: ToolbarActionsModifierItem): Boolean

    fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    )
}

sealed interface ToolbarActionsModifierItem {

    data class CloudDriveSyncs(
        val item: CloudDriveSyncsToolbarActionsModifierItem,
    ) : ToolbarActionsModifierItem

    data class AudioSection(
        val item: AudioToolbarActionsModifierItem,
    ) : ToolbarActionsModifierItem

    data class DocumentSection(
        val item: DocumentSectionToolbarActionsModifierItem,
    ) : ToolbarActionsModifierItem

    data class IncomingShares(
        val item: IncomingSharesToolbarActionsModifierItem,
    ) : ToolbarActionsModifierItem

    data class OutgoingShares(
        val item: OutgoingSharesToolbarActionsModifierItem,
    ) : ToolbarActionsModifierItem
}
