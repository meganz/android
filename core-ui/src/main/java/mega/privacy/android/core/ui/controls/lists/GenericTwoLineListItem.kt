package mega.privacy.android.core.ui.controls.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Generic two line list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param icon Icon
 * @param modifier Modifier
 * @param subTitlePrefixIcons Subtitle prefix
 * @param subTitleSuffixIcons Subtitle suffix
 * @param titleIcons Title icons
 * @param trailingIcons Body suffix
 */

@Composable
internal fun GenericTwoLineListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: @Composable (() -> Unit)? = null,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)? = null,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    trailingIcons: @Composable (RowScope.() -> Unit)? = null,
) {
    GenericTwoLineListItem(
        title = {
            Text(
                modifier = Modifier,
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MegaTheme.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitle = subtitle?.let {
            {
                Text(
                    modifier = Modifier,
                    text = subtitle,
                    style = MaterialTheme.typography.subtitle2,
                    color = MegaTheme.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        icon = icon,
        modifier = modifier,
        subTitlePrefixIcons = subTitlePrefixIcons,
        subTitleSuffixIcons = subTitleSuffixIcons,
        titleIcons = titleIcons,
        trailingIcons = trailingIcons,
    )
}

/**
 * Generic two line list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param icon Icon
 * @param modifier Modifier
 * @param subTitlePrefixIcons Subtitle prefix
 * @param subTitleSuffixIcons Subtitle suffix
 * @param titleIcons Title icons
 * @param trailingIcons Body suffix
 */
@Composable
internal fun GenericTwoLineListItem(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)? = null,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    trailingIcons: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        Column(
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            TitleRow(title, titleIcons)
            SubTitleRow(subtitle, subTitlePrefixIcons, subTitleSuffixIcons)
        }
        TrailingIcons(trailingIcons)
    }
}

@Composable
private fun RowScope.TrailingIcons(trailingIcons: @Composable (RowScope.() -> Unit)?) {
    CompositionLocalProvider(
        LocalContentColor provides MegaTheme.colors.icon.secondary,
        LocalContentAlpha provides 1f,
    ) {
        trailingIcons?.invoke(this)
    }
}

@Composable
private fun TitleRow(
    title: @Composable () -> Unit,
    titleIcons: @Composable (RowScope.() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            CompositionLocalProvider(
                LocalContentColor provides MegaTheme.colors.text.primary,
                LocalTextStyle provides MaterialTheme.typography.subtitle1,
            ) {
                title()
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides MegaTheme.colors.icon.secondary,
            LocalContentAlpha provides 1f,
        ) {
            titleIcons?.invoke(this)
        }

    }
}

@Composable
private fun SubTitleRow(
    subtitle: @Composable (() -> Unit)?,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)?,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)?,
) {
    if (subtitle != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MegaTheme.colors.icon.secondary,
                LocalContentAlpha provides 1f,
            ) {
                subTitlePrefixIcons?.invoke(this)
            }
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalContentColor provides MegaTheme.colors.text.secondary,
                    LocalTextStyle provides MaterialTheme.typography.subtitle2
                ) {
                    subtitle()
                }
            }
            CompositionLocalProvider(
                LocalContentColor provides MegaTheme.colors.icon.secondary,
                LocalContentAlpha provides 1f,
            ) {
                subTitleSuffixIcons?.invoke(this)
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListViewItemSimple() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(
            title = "Simple title",
            subtitle = "Simple sub title",
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_sync),
                    contentDescription = "Folder sync"
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListViewItemAllEnabled() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(
            title = "All enabled view title",
            subtitle = "All enabled view sub title",
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_sync),
                    contentDescription = "Folder sync"
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewGenericTwoLineListItemWithLongTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(
            title = "Title very big for testing the middle ellipsis",
            subtitle = "Subtitle very big for testing the middle ellipsis",
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_sync),
                    contentDescription = "Folder sync"
                )
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericListViewItemWithTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(title = "title")
    }
}