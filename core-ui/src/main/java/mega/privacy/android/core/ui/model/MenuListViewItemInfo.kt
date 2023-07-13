package mega.privacy.android.core.ui.model

import androidx.annotation.DrawableRes


/**
 * MenuListViewItemInfo
 *
 * data class for MenuListViewItem
 * @param text list item title
 * @param icon list icon if null the list will not be having prefix icon
 * @param addIconPadding should be true if we need to leave icon space before text
 * @param hasSwitch true if list item has switch
 * @param isChecked sets the default switch value
 * @param isDestructive if true the text will be displayed in destructive(red) colors
 */
data class MenuListViewItemInfo(
    val text: String,
    @DrawableRes val icon: Int? = null,
    val addIconPadding: Boolean = false,
    val isDestructive: Boolean = false,
    val hasSwitch: Boolean = false,
    val isChecked: Boolean = false,
)