package mega.privacy.android.app.presentation.validator.toolbaractions.modifier

import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import javax.inject.Inject

class CloudDriveSyncsToolbarActionsModifier @Inject constructor() : ToolbarActionsModifier {

    override fun canHandle(item: ToolbarActionsModifierItem): Boolean =
        item is ToolbarActionsModifierItem.CloudDriveSyncs

    override fun modify(
        control: CloudStorageOptionControlUtil.Control,
        item: ToolbarActionsModifierItem,
    ) {
        item as ToolbarActionsModifierItem.CloudDriveSyncs

        // Hidden Nodes
        if (!item.item.hiddenNodeItem.isEnabled) {
            control.hide().isVisible = false
            control.unhide().isVisible = false
        } else {
            control.hide().isVisible = item.item.hiddenNodeItem.canBeHidden
            control.unhide().isVisible = !item.item.hiddenNodeItem.canBeHidden
        }

        // Add To
        control.addToAlbum().isVisible = item.item.addToItem.canBeAddedToAlbum
        control.addTo().isVisible = item.item.addToItem.canBeAddedTo

        // Favourite
        control.addFavourites().isVisible = item.item.favouritesItem.canBeAdded
        control.removeFavourites().isVisible = item.item.favouritesItem.canBeRemoved

        // Labels
        control.addLabel().isVisible = item.item.addLabelItem.canBeAdded
    }
}
