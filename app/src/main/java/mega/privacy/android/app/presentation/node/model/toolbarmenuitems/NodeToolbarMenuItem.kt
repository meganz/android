package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node toolbar menu item
 */
interface NodeToolbarMenuItem<T : MenuAction> {


    /**
     * should display
     *
     * checks if menu item should be displayed or not
     *
     * @param hasNodeAccessPermission
     * @param selectedNodes
     * @param canBeMovedToTarget
     * @param noNodeInBackups
     * @param noNodeTakenDown
     * @param allFileNodes
     * @param resultCount
     * @return [Boolean]
     */
    fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: Set<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ): Boolean

    /**
     * Menu action item
     */
    val menuAction: T
}