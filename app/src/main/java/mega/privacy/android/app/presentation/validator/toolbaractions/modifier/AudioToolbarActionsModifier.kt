package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import javax.inject.Inject

class AudioToolbarActionsModifier @Inject constructor() : ToolbarActionsModifier {

    override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
        item is ToolbarActionsModifierItem.AudioSection

    override fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    ) {
        item as ToolbarActionsModifierItem.AudioSection

        if (!item.item.hiddenNodeItem.isEnabled) {
            control.hide().isVisible = false
            control.unhide().isVisible = false
        } else {
            control.hide().isVisible = item.item.hiddenNodeItem.canBeHidden
            control.unhide().isVisible = !item.item.hiddenNodeItem.canBeHidden
        }
    }
}
