package mega.privacy.android.shared.original.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Generic two line list item
 *
 * @param title Title
 * @param modifier The [Modifier]
 * @param subtitle Subtitle
 * @param titleTextColor [TextColor] to apply to the title. If no color set, this will be [TextColor.Primary].
 * @param subtitleTextColor [TextColor] to apply to the subtitle. If no color set, this will be [TextColor.Secondary].
 * @param showEntireSubtitle If true, the entire [subtitle] is displayed. Otherwise, only one line
 * is provided for the [subtitle]
 * @param fillTitleText If true, the title will fill the available space
 * @param fillSubTitleText If true, the subtitle will fill the available space
 * @param icon Icon
 * @param modifier Modifier
 * @param subTitlePrefixIcons Subtitle prefix
 * @param subTitleSuffixIcons Subtitle suffix
 * @param titleIcons Title icons
 * @param trailingIcons Body suffix
 * @param onItemClicked An optional item click listener
 */
@Composable
fun GenericTwoLineListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleTextColor: TextColor = TextColor.Primary,
    subtitleTextColor: TextColor = TextColor.Secondary,
    showEntireSubtitle: Boolean = false,
    fillTitleText: Boolean = false,
    fillSubTitleText: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)? = null,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    trailingIcons: @Composable (RowScope.() -> Unit)? = null,
    onItemClicked: (() -> Unit)? = null,
) {
    GenericTwoLineListItem(
        title = {
            Text(
                modifier = Modifier,
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MegaOriginalTheme.textColor(textColor = titleTextColor),
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
                    color = MegaOriginalTheme.textColor(textColor = subtitleTextColor),
                    maxLines = if (showEntireSubtitle) {
                        Int.MAX_VALUE
                    } else {
                        1
                    },
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
        onItemClicked = onItemClicked,
        fillTitleText = fillTitleText,
        fillSubTitleText = fillSubTitleText,
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
 * @param onItemClicked An optional item click listener
 */
@Composable
internal fun GenericTwoLineListItem(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    fillTitleText: Boolean = false,
    fillSubTitleText: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)? = null,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)? = null,
    titleIcons: @Composable (RowScope.() -> Unit)? = null,
    trailingIcons: @Composable (RowScope.() -> Unit)? = null,
    onItemClicked: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .conditional(onItemClicked != null) {
                clickable {
                    onItemClicked?.invoke()
                }
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.invoke()
        Column(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            TitleRow(title, titleIcons, fillTitleText)
            SubTitleRow(subtitle, subTitlePrefixIcons, subTitleSuffixIcons, fillSubTitleText)
        }
        TrailingIcons(trailingIcons)
    }
}

@Composable
internal fun RowScope.TrailingIcons(trailingIcons: @Composable (RowScope.() -> Unit)?) {
    CompositionLocalProvider(
        LocalContentColor provides MegaOriginalTheme.colors.icon.secondary,
        LocalContentAlpha provides 1f,
    ) {
        trailingIcons?.invoke(this)
    }
}

@Composable
internal fun TitleRow(
    title: @Composable () -> Unit,
    titleIcons: @Composable (RowScope.() -> Unit)?,
    fillTitleText: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.weight(1f, fill = fillTitleText)) {
            CompositionLocalProvider(
                LocalContentColor provides MegaOriginalTheme.colors.text.primary,
                LocalTextStyle provides MaterialTheme.typography.subtitle1,
            ) {
                title()
            }
        }
        CompositionLocalProvider(
            LocalContentColor provides MegaOriginalTheme.colors.icon.secondary,
            LocalContentAlpha provides 1f,
        ) {
            titleIcons?.invoke(this)
        }

    }
}

@Composable
internal fun SubTitleRow(
    subtitle: @Composable (() -> Unit)?,
    subTitlePrefixIcons: @Composable (RowScope.() -> Unit)?,
    subTitleSuffixIcons: @Composable (RowScope.() -> Unit)?,
    fillSubTitleText: Boolean,
) {
    if (subtitle != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MegaOriginalTheme.colors.icon.secondary,
                LocalContentAlpha provides 1f,
            ) {
                subTitlePrefixIcons?.invoke(this)
            }
            Box(modifier = Modifier.weight(1f, fill = fillSubTitleText)) {
                CompositionLocalProvider(
                    LocalContentColor provides MegaOriginalTheme.colors.text.secondary,
                    LocalTextStyle provides MaterialTheme.typography.subtitle2
                ) {
                    subtitle()
                }
            }
            CompositionLocalProvider(
                LocalContentColor provides MegaOriginalTheme.colors.icon.secondary,
                LocalContentAlpha provides 1f,
            ) {
                subTitleSuffixIcons?.invoke(this)
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun GenericTwoLineListItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(
            title = "Generic Two Line List Item Title",
            subtitle = "Generic Two Line List Item Subtitle",
            icon = {
                Icon(
                    painter = painterResource(id = IconPackR.drawable.ic_folder_sync_medium_solid),
                    contentDescription = "Folder sync"
                )
            }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericTwoLineListItemWithLongContentPreview(
    @PreviewParameter(BooleanProvider::class) showEntireSubtitle: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(
            title = "Very Long Generic Two Line List Item Title to Simulate Ellipsis",
            subtitle = "Very Long Generic Two Line List Item Subtitle to Simulate Ellipsis",
            showEntireSubtitle = showEntireSubtitle,
            icon = {
                Icon(
                    painter = painterResource(id = IconPackR.drawable.ic_folder_sync_medium_solid),
                    contentDescription = "Folder sync"
                )
            },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GenericTwoLineListItemWithOnlyTitlePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericTwoLineListItem(title = "Generic Two Line List Item Title")
    }
}