package mega.privacy.android.app.presentation.node.view.toolbar

import mega.privacy.android.core.ui.model.MenuAction

/**
 * State for toolbar
 * @property menuActions List of [MenuAction]
 */
data class ToolbarState(
    val menuActions: List<MenuAction> = emptyList(),
)