package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.model.TopAppBarAction
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as IconPackR

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
fun NodeActionListTile(
    menuAction: TopAppBarAction,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    trailingItem: (@Composable () -> Unit)? = null,
) {
    NodeActionListTile(
        text = menuAction.getDescription(),
        icon = menuAction.getIconPainter(),
        modifier = modifier,
        isDestructive = isDestructive,
        onActionClicked = onActionClicked,
        divider = null,
        trailingItem = trailingItem,
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
fun NodeActionListTile(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    addIconPadding: Boolean = true,
    isDestructive: Boolean = false,
    onActionClicked: (() -> Unit)? = null,
    divider: (@Composable () -> Unit)? = null,
    trailingItem: (@Composable () -> Unit)? = null,
) {
    Column {
        Row(
            modifier = modifier
                .height(56.dp)
                .fillMaxWidth()
                .clickable(onActionClicked != null) {
                    onActionClicked?.invoke()
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { testTagsAsResourceId = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                if (isDestructive) {
                    MegaIcon(
                        modifier = Modifier
                            .testTag(MENU_ITEM_ICON_TAG)
                            .padding(end = 32.dp)
                            .size(size = 24.dp),
                        painter = icon,
                        contentDescription = null,
                        supportTint = SupportColor.Error
                    )
                } else {
                    MegaIcon(
                        modifier = Modifier
                            .testTag(MENU_ITEM_ICON_TAG)
                            .padding(end = 32.dp)
                            .size(size = 24.dp),
                        painter = icon,
                        contentDescription = null,
                        tint = IconColor.Secondary
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                MegaText(
                    modifier = Modifier
                        .testTag(MENU_ITEM_TEXT_TAG)
                        .then(
                            if (icon != null && addIconPadding) {
                                Modifier.padding(start = 56.dp)
                            } else Modifier
                        ),
                    text = text,
                    textColor = if (isDestructive) TextColor.Error else TextColor.Primary,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
            trailingItem?.invoke()
        }

        divider?.invoke()
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
    AndroidThemeForPreviews {
        NodeActionListTile(
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
    AndroidThemeForPreviews {
        NodeActionListTile(
            text = "Menu Item",
            icon = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
        ) {
            Toggle(
                modifier = Modifier.testTag(MENU_ITEM_SWITCH_TAG),
                isChecked = true,
                onCheckedChange = {}
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMegaMenuActionWithoutIcon() {
    AndroidThemeForPreviews {
        NodeActionListTile(text = "Menu Item",)
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMegaMenuActionWithTextButton() {
    AndroidThemeForPreviews {
        NodeActionListTile(
            text = "Menu Item",
            icon = painterResource(id = IconPackR.drawable.ic_folder_medium_solid),
        ) {
            MegaText(
                text = "Button",
                style = MaterialTheme.typography.bodyLarge,
                textColor = TextColor.Accent,
            )
        }
    }
}
