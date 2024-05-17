package mega.privacy.android.shared.original.core.ui.model

/**
 * Menu action with click
 *
 * @property menuAction [MenuAction]
 * @property onClick action to be executed when the menu item is clicked
 */
data class MenuActionWithClick(
    val menuAction: MenuAction,
    val onClick: () -> Unit,
)
