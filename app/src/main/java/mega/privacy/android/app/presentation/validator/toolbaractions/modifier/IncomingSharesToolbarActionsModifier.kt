package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import android.view.MenuItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import javax.inject.Inject

class IncomingSharesToolbarActionsModifier @Inject constructor() : ToolbarActionsModifier {

    override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
        item is ToolbarActionsModifierItem.IncomingShares

    override fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    ) {
        item as ToolbarActionsModifierItem.IncomingShares

        control.hide().isVisible = false
        control.unhide().isVisible = false
        control.shareFolder().isVisible = false
        control.shareOut().isVisible = false

        if (item.item.renameItem.isEnabled) {
            control.rename().isVisible = true
            if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            } else {
                control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
            }
        }

        if (item.item.moveItem.isEnabled) {
            control.move().isVisible = true
            if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                control.move().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            } else {
                control.move().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
            }
        } else {
            control.move().isVisible = false
        }


        if (item.item.copyItem.isEnabled) {
            control.copy().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
        } else {
            control.saveToDevice().isVisible = false
        }

        control.trash().isVisible = item.item.moveItem.isEnabled
        control.addToAlbum().isVisible = false
        control.addTo().isVisible = false
    }
}
