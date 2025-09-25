package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import android.view.MenuItem
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import javax.inject.Inject

class OutgoingSharesToolbarActionsModifier @Inject constructor() : ToolbarActionsModifier {

    override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
        item is ToolbarActionsModifierItem.OutgoingShares

    override fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    ) {
        item as ToolbarActionsModifierItem.OutgoingShares

        control.move().isVisible = false
        control.hide().isVisible = false
        control.unhide().isVisible = false

        if (item.item.areAllNotTakenDown) {
            if (item.item.isRootLevel) {
                control.removeShare().setVisible(true).showAsAction =
                    MenuItem.SHOW_AS_ACTION_ALWAYS
            }

            control.shareOut().setVisible(true).showAsAction =
                MenuItem.SHOW_AS_ACTION_ALWAYS

            if (item.item.shouldHideLink) {
                control.manageLink().setVisible(false)
                control.removeLink().setVisible(false)
                control.link.setVisible(false)
            }

            control.copy().isVisible = true
            if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
            } else {
                control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
            }
        }

        control.addToAlbum().isVisible = item.item.addToItem.canBeAddedToAlbum
        control.addTo().isVisible = item.item.addToItem.canBeAddedTo
    }
}
