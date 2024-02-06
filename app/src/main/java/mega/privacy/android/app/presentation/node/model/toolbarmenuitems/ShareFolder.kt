package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Show share folder menu item
 *
 * @property menuAction [ShareFolderMenuAction]
 */
class ShareFolder @Inject constructor(
    override val menuAction: ShareFolderMenuAction
) : NodeToolbarMenuItem<MenuActionWithIcon> {


    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown && selectedNodes.isNotEmpty() && selectedNodes.first() is FolderNode

}
