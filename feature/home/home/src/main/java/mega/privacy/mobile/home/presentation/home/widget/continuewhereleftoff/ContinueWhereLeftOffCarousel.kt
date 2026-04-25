package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.continuewhereleftoff.iconForType

@Composable
internal fun ContinueWhereLeftOffCarousel(
    items: List<ContinueWhereLeftOffItem>,
    onItemClick: (ContinueWhereLeftOffItem) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(modifier = modifier) {
        ContinueWhereLeftOffHeader(onViewAllClick = onViewAllClick)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.nodeHandle }) { item ->
                ContinueWhereLeftOffCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun ContinueWhereLeftOffHeader(
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = stringResource(sharedR.string.home_widget_continue_where_left_off),
            style = AppTheme.typography.titleMedium,
            textColor = TextColor.Primary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .wrapContentSize(unbounded = true, align = Alignment.Center)
                .size(48.dp)
                .clickable { onViewAllClick() },
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.ChevronRight,
                contentDescription = null,
                tint = IconColor.Secondary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ContinueWhereLeftOffCard(
    item: ContinueWhereLeftOffItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = iconForType(item.type)),
                contentDescription = item.title,
                modifier = Modifier.size(48.dp),
            )
        }
        MegaText(
            text = item.title,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffCarouselPreview() {
    AndroidThemeForPreviews {
        ContinueWhereLeftOffCarousel(
            items = listOf(
                ContinueWhereLeftOffItem(
                    nodeHandle = 1L,
                    title = "Falastin36_press_trailer.mov",
                    type = RecentlyUsedType.Video,
                    lastAccessedTimestamp = 1712966400L,
                ),
                ContinueWhereLeftOffItem(
                    nodeHandle = 2L,
                    title = "Interview Agnes Varda.pdf",
                    type = RecentlyUsedType.PDF,
                    lastAccessedTimestamp = 1712880000L,
                ),
                ContinueWhereLeftOffItem(
                    nodeHandle = 3L,
                    title = "Annemarie_Jacir_notes.txt",
                    type = RecentlyUsedType.TextEditor,
                    lastAccessedTimestamp = 1712793600L,
                ),
                ContinueWhereLeftOffItem(
                    nodeHandle = 4L,
                    title = "Podcast Episode.mp3",
                    type = RecentlyUsedType.Audio,
                    lastAccessedTimestamp = 1712707200L,
                ),
                ContinueWhereLeftOffItem(
                    nodeHandle = 5L,
                    title = "Shared File Link",
                    type = RecentlyUsedType.FileLink,
                    lastAccessedTimestamp = 1712620800L,
                ),
                ContinueWhereLeftOffItem(
                    nodeHandle = 6L,
                    title = "Shared Folder",
                    type = RecentlyUsedType.FolderLink,
                    lastAccessedTimestamp = 1712534400L,
                ),
            ),
            onItemClick = {},
            onViewAllClick = {},
        )
    }
}
