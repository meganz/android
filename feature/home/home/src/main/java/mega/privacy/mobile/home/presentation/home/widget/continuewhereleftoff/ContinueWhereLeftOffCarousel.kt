package mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.dialog.TakeDownDialog
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.continuewhereleftoff.DurationBadge
import mega.privacy.mobile.home.presentation.continuewhereleftoff.iconForType

@Composable
internal fun ContinueWhereLeftOffCarousel(
    items: List<ContinueWhereLeftOffItem>,
    onItemClick: (ContinueWhereLeftOffItem) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTakenDownDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        ContinueWhereLeftOffHeader(
            onViewAllClick = onViewAllClick,
            showChevron = items.isNotEmpty(),
        )
        if (items.isEmpty()) {
            ContinueWhereLeftOffEmptyView()
        } else {
            val listState = rememberLazyListState()
            // Keyed items keep the scroll anchored to the previously visible item, so when a
            // new most recent item is added (or moved) to the front it stays off-screen.
            // Snap back to the start whenever the most recent item changes, regardless of the
            // current scroll position, so the most recent item is always shown (T21372416).
            // requestScrollToItem applies the snap on the next remeasure with the new data and
            // is dropped if the user interacts with the list before it lands.
            LaunchedEffect(items.first().nodeHandle) {
                if (!listState.isScrollInProgress) {
                    listState.requestScrollToItem(0)
                }
            }
            val visibleItems = items.take(CAROUSEL_MAX_VISIBLE_ITEMS)
            val showMore = items.size > CAROUSEL_MAX_VISIBLE_ITEMS
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = visibleItems,
                    key = { it.nodeHandle },
                    contentType = { it.type },
                ) { item ->
                    ContinueWhereLeftOffCard(
                        item = item,
                        onClick = {
                            // Taken-down files must not be opened/played; show the dispute
                            // dialog instead, mirroring the node lists.
                            if (item.isTakenDown) {
                                showTakenDownDialog = true
                            } else {
                                onItemClick(item)
                            }
                        },
                    )
                }
                if (showMore) {
                    item(key = CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG) {
                        ContinueWhereLeftOffMoreCard(onClick = onViewAllClick)
                    }
                }
            }
        }
    }

    if (showTakenDownDialog) {
        TakeDownDialog(
            isFolder = false,
            onDismiss = { showTakenDownDialog = false },
        )
    }
}

@Composable
private fun ContinueWhereLeftOffHeader(
    onViewAllClick: () -> Unit,
    showChevron: Boolean,
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
            style = AppTheme.typography.titleMedium.copy(fontSize = 18.sp),
            textColor = TextColor.Primary,
            modifier = Modifier.weight(1f),
        )
        if (showChevron) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(unbounded = true, align = Alignment.Center)
                    .size(48.dp)
                    .testTag(CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG)
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
}

@Composable
private fun ContinueWhereLeftOffEmptyView(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = stringResource(sharedR.string.home_cwlo_empty_state),
            style = AppTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            textColor = TextColor.Secondary,
            modifier = Modifier
                .weight(1f)
                .testTag(CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.ic_cwlo_empty_state),
            contentDescription = null,
            modifier = Modifier.size(60.dp),
        )
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
            // Dim the whole card for hidden items so icon-only files (e.g. text) are also
            // visibly marked, matching the Recents list and the shared node items; the
            // thumbnail blur below only affects real image previews.
            .alpha(if (item.isSensitive) 0.5f else 1f)
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
            NodeThumbnailView(
                // Taken-down nodes must not show their original thumbnail; passing null data
                // falls back to the generic file-type icon, mirroring the node lists.
                data = ThumbnailRequest(NodeId(item.nodeHandle)).takeIf { !item.isTakenDown },
                defaultImage = iconForType(item.type),
                contentDescription = item.title,
                layoutType = ThumbnailLayoutType.Grid,
                blurImage = item.isSensitive,
                // Center the thumbnail rather than forcing it to match the box: a real
                // thumbnail still fills the box (NodeThumbnailView applies fillMaxSize on
                // success), while a file-type icon stays at its intended placeholder size
                // instead of being stretched to the box dimensions (AND-23926).
                modifier = Modifier.align(Alignment.Center),
            )
            item.duration?.takeIf { it.isNotEmpty() }?.let { duration ->
                DurationBadge(
                    duration = duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MegaText(
                text = item.title,
                // Taken-down nodes are marked in error red, mirroring the node lists.
                textColor = if (item.isTakenDown) TextColor.Error else TextColor.Primary,
                style = AppTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.isTakenDown) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.AlertTriangle,
                    contentDescription = "Dispute taken down",
                    tint = SupportColor.Error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ContinueWhereLeftOffMoreCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moreText =
        stringResource(sharedR.string.album_content_selection_action_more_description)
    Box(
        modifier = modifier
            .width(96.dp)
            .testTag(CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // Reserve the same height as a real card (90dp thumbnail box + title row) so the "More"
        // label is centered vertically across the full card height, including the title area.
        // Hidden from accessibility so the visible label below is the only one announced.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics { },
        ) {
            Spacer(modifier = Modifier.height(90.dp))
            MegaText(
                text = moreText,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = Modifier
                    .alpha(0f)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        MegaText(
            text = moreText,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
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
            ),
            onItemClick = {},
            onViewAllClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffCarouselWithMorePreview() {
    AndroidThemeForPreviews {
        ContinueWhereLeftOffCarousel(
            items = (1L..9L).map { handle ->
                ContinueWhereLeftOffItem(
                    nodeHandle = handle,
                    title = "Item $handle.pdf",
                    type = RecentlyUsedType.PDF,
                    lastAccessedTimestamp = 1712880000L + handle,
                )
            },
            onItemClick = {},
            onViewAllClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContinueWhereLeftOffCarouselEmptyPreview() {
    AndroidThemeForPreviews {
        ContinueWhereLeftOffCarousel(
            items = emptyList(),
            onItemClick = {},
            onViewAllClick = {},
        )
    }
}

// The carousel renders at most this many cards; when there are more items a "More" tile is
// appended that opens the full list (T21373295).
private const val CAROUSEL_MAX_VISIBLE_ITEMS = 8

internal const val CONTINUE_WHERE_LEFT_OFF_EMPTY_TEXT_TEST_TAG =
    "continue_where_left_off_widget:empty_text"
internal const val CONTINUE_WHERE_LEFT_OFF_LIST_TEST_TAG =
    "continue_where_left_off_widget:list"
internal const val CONTINUE_WHERE_LEFT_OFF_CHEVRON_TEST_TAG =
    "continue_where_left_off_widget:chevron"
internal const val CONTINUE_WHERE_LEFT_OFF_MORE_TEST_TAG =
    "continue_where_left_off_widget:more"
