package mega.privacy.android.core.ui.controls.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.core.ui.model.MenuListViewItemInfo
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * MenuListViewItem
 *
 * One line list menu item from Figma Designs
 * @param menuListViewItemInfo data for populating menu list view
 * @param onItemClick list item click
 * @param modifier
 */
@Composable
fun MenuListViewItem(
    menuListViewItemInfo: MenuListViewItemInfo,
    modifier: Modifier = Modifier,
    onItemClick: (() -> Unit?)? = null,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .conditional(onItemClick != null) {
                clickable {
                    onItemClick?.invoke()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (menuListViewItemInfo.icon != null) {
            Icon(
                modifier = Modifier
                    .testTag(MENU_ITEM_ICON_TAG)
                    .padding(end = 16.dp)
                    .size(size = 24.dp),
                painter = painterResource(id = menuListViewItemInfo.icon),
                contentDescription = null,
                tint = if (menuListViewItemInfo.isDestructive) MaterialTheme.colors.red_600_red_300 else MaterialTheme.colors.textColorSecondary,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier
                    .testTag(MENU_ITEM_TEXT_TAG)
                    .conditional(menuListViewItemInfo.addIconPadding) {
                        padding(start = 56.dp)
                    },
                text = menuListViewItemInfo.text,
                style = MaterialTheme.typography.subtitle1.copy(
                    color = if (menuListViewItemInfo.isDestructive) MaterialTheme.colors.red_600_red_300 else MaterialTheme.colors.textColorPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (menuListViewItemInfo.hasSwitch) {
            MegaSwitch(
                modifier = Modifier.testTag(MENU_ITEM_SWITCH_TAG),
                checked = menuListViewItemInfo.isChecked,
                onCheckedChange = {
                    onItemClick?.invoke()
                }
            )
        }
    }
}

internal const val MENU_ITEM_ICON_TAG = "menu_list_view_item:list_icon:"
internal const val MENU_ITEM_TEXT_TAG = "menu_list_view_item:text_title"
internal const val MENU_ITEM_SWITCH_TAG = "menu_list_view_item:button_switch"

@CombinedThemePreviews
@Composable
private fun PreviewMenuHeaderListViewItem(
    @PreviewParameter(MenuListViewItemPreviewProvider::class) menuListViewItemInfo: MenuListViewItemInfo,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuListViewItem(
            menuListViewItemInfo = menuListViewItemInfo, onItemClick = {}
        )
    }
}

private class MenuListViewItemPreviewProvider :
    PreviewParameterProvider<MenuListViewItemInfo> {
    override val values: Sequence<MenuListViewItemInfo>
        get() = listOf(
            MenuListViewItemInfo(
                text = "Menu Item"
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                isDestructive = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                isDestructive = true,
                addIconPadding = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
                isDestructive = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = true
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = true,
                isDestructive = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = false,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = false,
                isDestructive = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = true,
                addIconPadding = true
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = true,
                isDestructive = true,
                addIconPadding = true
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = false,
                addIconPadding = true
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                hasSwitch = true,
                isChecked = false,
                isDestructive = true,
                addIconPadding = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
                hasSwitch = true,
                isChecked = true
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
                hasSwitch = true,
                isChecked = true,
                isDestructive = true,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
                hasSwitch = true,
                isChecked = false,
            ),
            MenuListViewItemInfo(
                text = "Menu Item",
                icon = R.drawable.ic_folder_list,
                hasSwitch = true,
                isChecked = false,
                isDestructive = true,
            ),
        ).asSequence()
}

