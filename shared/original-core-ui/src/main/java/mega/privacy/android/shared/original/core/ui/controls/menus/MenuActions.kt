package mega.privacy.android.shared.original.core.ui.controls.menus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.google.android.material.R
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.appbar.LocalMegaAppBarColors
import mega.privacy.android.shared.original.core.ui.controls.appbar.addClick
import mega.privacy.android.shared.original.core.ui.controls.tooltips.Tooltip
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon


/**
 * Utility function to generate actions for [TopAppBar]
 * @param actions the actions to be added
 * @param maxActionsToShow if there are more actions than that the extra actions will be shown in a drop down menu
 * @param enabled if false, all actions will be disabled, if true each [MenuAction.enabled] will be used to check if the action is enabled or not
 * @param onActionClick event to receive clicks on actions, both in the toolbar or in the drop down menu
 */
@Composable
fun RowScope.MenuActions(
    actions: List<MenuAction>,
    maxActionsToShow: Int,
    onActionClick: (MenuAction) -> Unit,
    enabled: Boolean = true,
) = MenuActions(
    actions.addClick(onActionClick) ?: emptyList(),
    maxActionsToShow,
    enabled
)

/**
 * Utility function to generate actions for [TopAppBar]
 * @param actions the actions to be added
 * @param maxActionsToShow if there are more actions than that the extra actions will be shown in a drop down menu
 * @param enabled if false, all actions will be disabled, if true each [MenuAction.enabled] will be used to check if the action is enabled or not
 */
@Composable
fun RowScope.MenuActions(
    actions: List<MenuActionWithClick>,
    maxActionsToShow: Int,
    enabled: Boolean = true,
) {
    val sortedActions = actions.sortedBy { it.menuAction.orderInCategory }
    val visible =
        sortedActions.filter { it.menuAction is MenuActionWithIcon }.take(maxActionsToShow)
    visible.forEach { (menuAction, onActionClick) ->
        IconButtonForAction(
            menuAction = menuAction as MenuActionWithIcon,
            onActionClick = onActionClick,
            enabled = enabled && menuAction.enabled,
        )
    }
    sortedActions.filterNot { visible.contains(it) }.takeIf { it.isNotEmpty() }?.let { notVisible ->
        OverflowDropDown(
            actions = notVisible,
            enabled = enabled,
        )
    }
}

@Composable
private fun IconButtonForAction(
    menuAction: MenuActionWithIcon,
    onActionClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButtonWithTooltip(
        iconPainter = menuAction.getIconPainter(),
        description = menuAction.getDescription(),
        onClick = { onActionClick() },
        modifier = Modifier.testTag(menuAction.testTag),
        enabled = enabled,
        highlightIconColor = menuAction.highlightIcon,
    )
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RowScope.OverflowDropDown(
    actions: List<MenuActionWithClick>,
    enabled: Boolean = true,
) {
    var showMoreMenu by remember {
        mutableStateOf(false)
    }
    Box(contentAlignment = Alignment.BottomEnd) {
        IconButtonWithTooltip(
            iconPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
            description = stringResource(id = R.string.abc_action_menu_overflow_description),
            onClick = { showMoreMenu = !showMoreMenu },
            modifier = Modifier.testTag(TAG_MENU_ACTIONS_SHOW_MORE),
            enabled = enabled,
        )
    }
    // Overflow menu, as opposed to an ordinary DropdownMenu, is show on top of the icons. With this top 0 size box we do the trick
    Box(modifier = Modifier.align(Alignment.Top)) {
        DropdownMenu(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .background(DSTokens.colors.background.surface1),
            expanded = showMoreMenu,
            onDismissRequest = {
                showMoreMenu = false
            }
        ) {
            actions.forEach { (menuAction, onActionClick) ->
                DropdownMenuItem(
                    onClick = {
                        onActionClick()
                        showMoreMenu = false
                    },
                    modifier = Modifier.testTag(menuAction.testTag)
                ) {
                    Text(text = menuAction.getDescription())
                }
            }
        }
    }
}

/**
 * IconButton with a tooltip that shows the [description] when long clicked
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IconButtonWithTooltip(
    iconPainter: Painter,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlightIconColor: Boolean = false,
) {
    Box(modifier = modifier) {
        val showTooltip = remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(radius = 24.dp),
                    role = Role.Button,
                    onClick = onClick,
                    enabled = enabled,
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
                tint = when {
                    enabled && highlightIconColor -> DSTokens.colors.icon.accent
                    enabled -> LocalMegaAppBarColors.current.iconsTintColor
                    else -> DSTokens.colors.icon.disabled
                }
            )
        }
        Tooltip(showTooltip, description)
    }
}

/**
 * test tag for the "Show more" menu actions button
 */
const val TAG_MENU_ACTIONS_SHOW_MORE = "menuActionsShowMore"