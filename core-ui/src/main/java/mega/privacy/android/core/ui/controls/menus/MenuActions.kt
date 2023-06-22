package mega.privacy.android.core.ui.controls.menus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.android.material.R
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
    val sortedActions = actions.sortedBy { it.orderInCategory }
    val visible = sortedActions
        .filterIsInstance(MenuActionWithIcon::class.java)
        .take(maxActionsToShow)
    visible.forEach {
        IconButtonForAction(
            menuAction = it,
            tint = tint,
            onActionClick = onActionClick
        )
    }
    sortedActions.filterNot { visible.contains(it) }.takeIf { it.isNotEmpty() }?.let { notVisible ->
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
    IconButtonWithTooltip(
        iconPainter = menuAction.getIconPainter(),
        description = menuAction.getDescription(),
        tint = tint,
        onClick = { onActionClick(menuAction) },
        modifier = Modifier.testTag(menuAction.getDescription())
    )
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
    IconButtonWithTooltip(
        iconPainter = painter,
        description = stringResource(id = R.string.abc_action_menu_overflow_description),
        tint = tint,
        onClick = { showMoreMenu = !showMoreMenu },
        modifier = Modifier.testTag(TAG_MENU_ACTIONS_SHOW_MORE)
    )

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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconButtonWithTooltip(
    iconPainter: Painter,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val showTooltip = remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(radius = 24.dp),
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTooltip.value = true
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = description,
                tint = tint,
            )
        }
        Tooltip(showTooltip) {
            Text(description)
        }
    }
}

/**
 * test tag for the "Show more" menu actions button
 */
const val TAG_MENU_ACTIONS_SHOW_MORE = "menuActionsShowMore"