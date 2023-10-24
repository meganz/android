package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchType

/**
 * Node toolbar menu item without icon
 */
interface NodeToolbarMenuItem<T : MenuAction> {


    /**
     * should display
     *
     * checks if menu item should be displayed or not
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