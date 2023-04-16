package mega.privacy.android.core.ui.controls

import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon


/**
 * Utility function to generate actions for [TopAppBar]
 * @param actions the actions to be added
 * @param maxActionsToShow if there are more actions than that the extra actions will be shown in a drop down menu
 * @param dropDownIcon an icon for the drop down
 * @param tint the tint color for the icons, including [dropDownIcon]
 * @param onActionClick event to recieve clicks on actions, both in the toolbar or in the drop down menu
 */
@Composable
fun MenuActions(
    actions: List<MenuAction>,
    maxActionsToShow: Int,
    dropDownIcon: Painter,
    tint: Color,
    onActionClick: (MenuAction) -> Unit,
) {
    val visible = actions
        .filterIsInstance(MenuActionWithIcon::class.java)
        .take(maxActionsToShow)
    visible.forEach {
        IconButtonForAction(
            menuAction = it,
            tint = tint,
            onActionClick = onActionClick
        )
    }
    actions.filterNot { visible.contains(it) }.takeIf { it.isNotEmpty() }?.let { notVisible ->
        DropDown(
            actions = notVisible,
            tint = tint,
            painter = dropDownIcon,
            onActionClick = onActionClick,
        )
    }

}

@Composable
private fun IconButtonForAction(
    menuAction: MenuActionWithIcon,
    tint: Color,
    onActionClick: (MenuAction) -> Unit,
) {
    IconButton(
        modifier = Modifier.testTag(menuAction.getDescription()),
        onClick = { onActionClick(menuAction) }) {
        Icon(
            painter = menuAction.getIconPainter(),
            contentDescription = menuAction.getDescription(),
            tint = tint,
        )
    }
}


@Composable
private fun DropDown(
    actions: List<MenuAction>,
    tint: Color,
    painter: Painter,
    onActionClick: (MenuAction) -> Unit,
) {
    var showMoreMenu by remember {
        mutableStateOf(false)
    }
    IconButton(
        modifier = Modifier.testTag(TAG_MENU_ACTIONS_SHOW_MORE),
        onClick = { showMoreMenu = !showMoreMenu },
    ) {
        Icon(
            painter = painter,
            contentDescription = stringResource(id = R.string.abc_action_menu_overflow_description),
            tint = tint,
        )
    }

    DropdownMenu(
        expanded = showMoreMenu,
        onDismissRequest = {
            showMoreMenu = false
        }
    ) {
        actions.forEach {
            DropdownMenuItem(onClick = {
                onActionClick(it)
                showMoreMenu = false
            }
            ) {
                Text(text = it.getDescription())
            }
        }
    }
}

/**
 * test tag for the "Show more" menu actions button
 */
const val TAG_MENU_ACTIONS_SHOW_MORE = "menuActionsShowMore"