package mega.privacy.android.shared.original.core.ui.controls.lists

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
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.controls.text.HighlightedText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor


/**
 * Generic three line list item
 *
 * @param title Title
 * @param subtitle Subtitle
 * @param description Simple description under Subtitle
 * @param icon Icon
 * @param modifier Modifier
 * @param subTitlePrefixIcons Subtitle prefix
 * @param subTitleSuffixIcons Subtitle suffix
 * @param titleIcons Title icons
 * @param trailingIcons Body suffix
 * @param onItemClicked An optional item click listener
 */
@Composable
internal fun GenericThreeLineListItem(
    title: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
    description: @Composable (() -> Unit)? = null,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 4.dp, top = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TitleRow(title, titleIcons, fillTitleText)
            SubTitleRow(subtitle, subTitlePrefixIcons, subTitleSuffixIcons, fillSubTitleText)
            DescriptionTitleRow(description)
        }
        Row(modifier = Modifier.padding(top = 12.dp)) {
            TrailingIcons(trailingIcons)
        }
    }
}

@Composable
private fun DescriptionTitleRow(
    description: @Composable (() -> Unit)?,
) {
    description?.let {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(
                    LocalContentColor provides MegaOriginalTheme.colors.text.secondary,
                    LocalTextStyle provides MaterialTheme.typography.subtitle2,
                ) {
                    description()
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun GenericThreeLineListItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        GenericThreeLineListItem(
            title = {
                Text(
                    text = "Very Long Item Title to Simulate Ellipsis"
                )
            },
            subtitle = {
                Text(
                    text = "Very Long Item Subtitle to Simulate Ellipsis"
                )
            },
            description = {
                HighlightedText(
                    text = "This is a very good description",
                    highlightText = "GOOD",
                    highlightBold = true,
                    textColor = TextColor.Primary,
                )
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_sync_medium_solid),
                    contentDescription = "Icon"
                )
            },
        )
    }
}
