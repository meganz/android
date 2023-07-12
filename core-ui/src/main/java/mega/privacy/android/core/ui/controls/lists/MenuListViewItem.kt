package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * MenuListViewItem
 *
 * One line list menu item from Figma Designs
 * ModalItemWithSwitch, ModalItemWithoutIcon, ModalItem
 * @param text list item title
 * @param icon list icon if null the list will not be having prefix icon
 * @param addIconPadding should be true if we need to leave icon space before text
 * @param onItemClick list item click
 * @param hasSwitch true if list item has switch
 * @param isChecked sets the default switch value
 */
@Composable
fun MenuListViewItem(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    addIconPadding: Boolean = false,
    hasSwitch: Boolean = false,
    isChecked: Boolean = false,
    onItemClick: (() -> Unit?)? = null,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .padding(16.dp)
            .fillMaxWidth()
            .indication(
                interactionSource = MutableInteractionSource(),
                indication = rememberRipple(bounded = true),
            )
            .clickable {
                onItemClick?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .testTag(ICON_TAG)
                    .padding(end = 16.dp)
                    .size(size = 24.dp),
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colors.textColorSecondary,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Text(
                modifier = Modifier
                    .testTag(TEXT_TAG)
                    .conditional(addIconPadding) {
                        padding(start = 56.dp)
                    },
                text = text,
                style = MaterialTheme.typography.subtitle1.copy(
                    color = MaterialTheme.colors.textColorPrimary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hasSwitch) {
            MegaSwitch(
                modifier = Modifier.testTag(SWITCH_TAG),
                checked = isChecked,
                onCheckedChange = {
                    onItemClick?.invoke()
                }
            )
        }
    }
}

internal const val ICON_TAG = "menu_list_view_item:list_icon:"
internal const val TEXT_TAG = "menu_list_view_item:text_title"
internal const val SWITCH_TAG = "menu_list_view_item:button_switch"

@CombinedThemePreviews
@Composable
private fun PreviewModalItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuListViewItem(
            text = "Menu item label test very big item ",
            icon = R.drawable.ic_favorite,
            onItemClick = null
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewModalItemWithSwitch(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuListViewItem(
            text = "Menu item label test very big item ",
            icon = R.drawable.ic_favorite,
            onItemClick = null,
            hasSwitch = true,
            isChecked = initialValue,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewModalItemWithoutIcon() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuListViewItem(
            text = "Menu item label test very big item",
            onItemClick = null
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewModalItemWithIconPadding() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuListViewItem(
            text = "Menu item label test very big item",
            onItemClick = null,
            addIconPadding = true
        )
    }
}