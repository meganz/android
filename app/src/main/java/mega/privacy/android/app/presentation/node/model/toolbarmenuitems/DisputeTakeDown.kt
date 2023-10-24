package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Dispute menu item
 *
 * @property menuAction [DisputeTakeDownMenuAction]
 */
class DisputeTakeDown @Inject constructor(
    override val menuAction: DisputeTakeDownMenuAction,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.size == 1
            && noNodeTakenDown.not()
}