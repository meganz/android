package mega.privacy.android.app.presentation.node.view

import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.ToolbarClickHandler
import mega.privacy.android.shared.original.core.ui.model.MenuAction

/**
 * Toolbar menu item
 *
 * @property action [MenuAction]
 * @property control [ToolbarClickHandler]
 */
data class ToolbarMenuItem(
    val action: MenuAction,
    val control: ToolbarClickHandler,
)