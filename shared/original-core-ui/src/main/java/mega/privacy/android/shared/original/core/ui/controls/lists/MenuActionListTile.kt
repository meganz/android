package mega.privacy.android.shared.original.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Menu action list tile
 *
 * @param menuAction action represented by this list tile
 * @param isDestructive if true the text will be displayed in destructive(red) colors
 * @param onActionClicked trigger item click on menu action
 * @param trailingItem composable widget which will be placed at the trailing end
 * @param modifier
 */
@Composable
fun MenuActionListTile(
    menuAction: MenuAction,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    trailingItem: (@Composable () -> Unit)? = null,
) {
    MenuActionListTile(
        text = menuAction.getDescription(),
        icon = (menuAction as? MenuActionWithIcon)?.getIconPainter(),
        modifier = modifier,
        isDestructive = isDestructive,
        onActionClicked = onActionClicked,
        trailingItem = trailingItem,
        dividerType = null,
    )
}

/**
 * MenuActionListTile
 *
 * One line list menu item from Figma Designs
 * @param text list item title
 * @param icon list icon if null the list will not be having prefix icon
 * @param addIconPadding should be true if we need to leave icon space before text
 * @param isDestructive if true the text will be displayed in destructive(red) colors
 * @param dividerType type for the divider below menu action. Hidden if NULL
 * @param onActionClicked trigger item click on menu action
 * @param trailingItem composable widget which will be placed at the trailing end
 * @param modifier
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MenuActionListTile(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    addIconPadding: Boolean = true,
    isDestructive: Boolean = false,
    dividerType: DividerType? = DividerType.BigStartPadding,
    onActionClicked: (() -> Unit)? = null,
    trailingItem: (@Composable () -> Unit)? = null,
) {
    Column {
        Row(
            modifier = modifier
                .height(56.dp)
                .fillMaxWidth()
                .conditional(onActionClicked != null) {
                    clickable {
                        onActionClicked?.invoke()
                    }
                }
                .padding(16.dp)
                .semantics { testTagsAsResourceId = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier
                        .testTag(MENU_ITEM_ICON_TAG)
                        .padding(end = 32.dp)
                        .size(size = 24.dp),
                    painter = icon,
                    contentDescription = null,
                    tint = if (isDestructive) {
                        MegaOriginalTheme.colors.support.error
                    } else {
                        MegaOriginalTheme.colors.icon.secondary
                    },
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MegaText(
                    modifier = Modifier
                        .testTag(MENU_ITEM_TEXT_TAG)
                        .conditional(icon == null && addIconPadding) {
                            padding(start = 56.dp)
                        },
                    text = text,
                    textColor = if (isDestructive) TextColor.Error else TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1,
                    overflow = LongTextBehaviour.Ellipsis(1)
                )
            }
            trailingItem?.invoke()
        }

        dividerType?.let {
            MegaDivider(dividerType = it)
        }
    }
}

internal const val MENU_ITEM_ICON_TAG = "menu_action:list_icon:"
internal const val MENU_ITEM_TEXT_TAG = "menu_action:text_title"
internal const val MENU_ITEM_SWITCH_TAG = "menu_action:button_switch"

@CombinedThemePreviews
@Composable
private fun PreviewPreviewMegaMenuAction(
    @PreviewParameter(BooleanProvider::class) isDestructive: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTile(
            text = "Menu Item",
            icon = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
            isDestructive = isDestructive,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMegaMenuActionWithSwitch(
    @PreviewParameter(BooleanProvider::class) hasSwitch: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTile(
            text = "Menu Item",
            icon = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
        ) {
            // The MegaSwitch from :core-ui is not ready yet. Please use it instead when it is ready.
            Switch(
                modifier = Modifier.testTag(MENU_ITEM_SWITCH_TAG),
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMegaMenuActionWithoutIcon() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTile(
            text = "Menu Item",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMegaMenuActionWithTextButton() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MenuActionListTile(
            text = "Menu Item",
            icon = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
        ) {
            Text(
                text = "Button",
                style = MaterialTheme.typography.button,
                color = MegaOriginalTheme.colors.text.accent,
            )
        }
    }
}
