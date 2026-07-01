package mega.privacy.android.feature.photos.presentation.timeline.revamp

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.TimelineGridSizeSettingsMenu
import mega.privacy.android.feature.photos.extensions.photosZoomGestureDetector
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.component.PhotoNodeBodyV2
import mega.privacy.android.feature.photos.presentation.timeline.component.MediaSkeletonView
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.nodes.dialog.TakeDownDialog
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineRevampScreen(
    uiState: TimelineRevampUiState,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onNodeClicked: (PhotosNodeContentItemV2?) -> Unit,
    onTakenDownDialogEventConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTakenDownDialog by rememberSaveable { mutableStateOf(false) }
    val takenDownDialogEvent =
        (uiState as? TimelineRevampUiState.Data)?.takenDownDialogEvent ?: consumed
    EventEffect(event = takenDownDialogEvent, onConsumed = onTakenDownDialogEventConsumed) {
        showTakenDownDialog = true
    }

    when (uiState) {
        is TimelineRevampUiState.Loading -> {
            MediaSkeletonView(
                modifier = modifier.testTag(TIMELINE_REVAMP_LOADING_SKELETON_TAG),
            )
        }

        is TimelineRevampUiState.Empty -> {
            EmptyStateView(
                modifier = Modifier.testTag(TIMELINE_REVAMP_EMPTY_VIEW_TAG),
                imagePainter = painterResource(R.drawable.il_glass_image),
                title = stringResource(sharedR.string.timeline_tab_empty_body_no_media_found)
            )
        }

        is TimelineRevampUiState.Data -> {
            TimelineRevampContent(
                sections = uiState.sections,
                sectionStartOffsets = uiState.sectionStartOffsets,
                loadedNodes = uiState.loadedNodes,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                gridSize = uiState.gridSize,
                onVisibleRangeChanged = onVisibleRangeChanged,
                onGridSizeChange = onGridSizeChange,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onNodeClicked = onNodeClicked,
                modifier = modifier,
            )
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
private fun TimelineRevampContent(
    sections: List<MediaTimelineSection>,
    sectionStartOffsets: List<Int>,
    loadedNodes: Map<Int, PhotosNodeContentItemV2>,
    isHiddenNodesEnabled: Boolean,
    gridSize: TimelineGridSize,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onNodeClicked: (PhotosNodeContentItemV2?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyGridState = rememberLazyGridState()
    val columns =
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridSize.portrait
        } else {
            gridSize.landscape
        }

    NotifyVisibleMediaRange(
        gridState = lazyGridState,
        onVisibleRangeChanged = onVisibleRangeChanged
    )

    FastScrollLazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .photosZoomGestureDetector(
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
            )
            .testTag(TIMELINE_REVAMP_CONTENT_GRID_TAG),
        state = lazyGridState,
        totalItems = sections.sumOf { it.count }.toInt() + sections.size,
    ) {
        sections.forEachIndexed { sectionIndex, section ->
            val base = sectionStartOffsets[sectionIndex]

            item(
                key = "${HEADER_KEY_PREFIX}${section.groupId}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                TimelineRevampSectionHeader(
                    title = section.groupId,
                    // The grid-size selector is only shown on the first header, matching the tab.
                    showGridSizeMenu = sectionIndex == 0,
                    gridSize = gridSize,
                    onGridSizeChange = onGridSizeChange,
                )
            }

            items(
                count = section.count.toInt(),
                key = { index -> "$MEDIA_KEY_PREFIX${base + index}" },
            ) { index ->
                val node = loadedNodes[base + index]
                PhotoNodeBodyV2(
                    node = node,
                    modifier = Modifier
                        .animateItem()
                        .padding(all = 1.dp),
                    shouldShowFavourite = node?.isFavourite == true,
                    isHiddenNodesEnabled = isHiddenNodesEnabled,
                    onClick = { onNodeClicked(node) },
                )
            }
        }
    }
}

@Composable
private fun TimelineRevampSectionHeader(
    title: String,
    showGridSizeMenu: Boolean,
    gridSize: TimelineGridSize,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
            .testTag("$TIMELINE_REVAMP_SECTION_HEADER_TAG$title"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTheme.typography.titleMedium,
            textColor = TextColor.Primary,
        )

        if (showGridSizeMenu) {
            TimelineRevampGridSizeMenu(
                gridSize = gridSize,
                onGridSizeChange = onGridSizeChange,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun TimelineRevampGridSizeMenu(
    gridSize: TimelineGridSize,
    onGridSizeChange: (TimelineGridSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        val gridSizeIcon = when (gridSize) {
            TimelineGridSize.Large -> IconPack.Small.Thin.Outline.Square
            TimelineGridSize.Default -> IconPack.Small.Thin.Outline.Grid4
            TimelineGridSize.Compact -> IconPack.Small.Thin.Outline.Grid9
        }
        MegaIcon(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .testTag(TIMELINE_REVAMP_GRID_SIZE_ICON_TAG),
            imageVector = gridSizeIcon,
            tint = IconColor.Secondary,
            contentDescription = "Change grid size, current size is : ${gridSize.name}",
        )

        TimelineGridSizeSettingsMenu(
            modifier = Modifier
                .widthIn(min = 220.dp)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MegaText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 6.dp),
                text = stringResource(sharedR.string.timeline_tab_grid_size_menu_title),
                style = AppTheme.typography.labelLarge,
                textColor = TextColor.Secondary,
            )

            TimelineGridSize.entries.reversed().forEach {
                DropdownMenuItem(
                    text = {
                        MegaText(
                            text = stringResource(it.nameResId),
                            style = AppTheme.typography.bodyLarge,
                            textColor = TextColor.Primary,
                        )
                    },
                    leadingIcon = {
                        if (gridSize == it) {
                            MegaIcon(
                                imageVector = IconPack.Medium.Thin.Outline.Check,
                                tint = IconColor.Primary,
                                contentDescription = null,
                            )
                        } else {
                            Box(modifier = Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        onGridSizeChange(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Observes which media slots are currently visible and reports the global index range to the
 * ViewModel so it can lazily load that window.
 */
@Composable
private fun NotifyVisibleMediaRange(
    gridState: LazyGridState,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
) {
    LaunchedEffect(gridState, onVisibleRangeChanged) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .mapNotNull { (it.key as? String)?.removePrefix(MEDIA_KEY_PREFIX)?.toIntOrNull() }
        }
            .distinctUntilChanged()
            .collect { visibleIndices ->
                if (visibleIndices.isNotEmpty()) {
                    onVisibleRangeChanged(visibleIndices.min(), visibleIndices.max())
                }
            }
    }
}

private const val HEADER_KEY_PREFIX = "header_"
private const val MEDIA_KEY_PREFIX = "media_"

internal const val TIMELINE_REVAMP_CONTENT_GRID_TAG = "timeline_revamp_content:grid"
internal const val TIMELINE_REVAMP_SECTION_HEADER_TAG = "timeline_revamp_content:section_header_"
internal const val TIMELINE_REVAMP_GRID_SIZE_ICON_TAG = "timeline_revamp_content:grid_size_icon"
internal const val TIMELINE_REVAMP_LOADING_SKELETON_TAG = "timeline_revamp_content:loading_skeleton"
internal const val TIMELINE_REVAMP_EMPTY_VIEW_TAG = "timeline_revamp_content:empty_view"

@CombinedThemePreviews
@Composable
private fun TimelineRevampScreenPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Data(
                sections = listOf(
                    MediaTimelineSection(
                        groupId = "May 2026",
                        startDate = 0L,
                        endDate = 0L,
                        count = 7,
                    ),
                    MediaTimelineSection(
                        groupId = "April 2026",
                        startDate = 0L,
                        endDate = 0L,
                        count = 4,
                    ),
                ),
                sectionStartOffsets = listOf(0, 7),
                loadedNodes = emptyMap(),
            ),
            onVisibleRangeChanged = { _, _ -> },
            onGridSizeChange = {},
            onZoomIn = {},
            onZoomOut = {},
            onNodeClicked = {},
            onTakenDownDialogEventConsumed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TimelineRevampEmptyPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Empty,
            onVisibleRangeChanged = { _, _ -> },
            onGridSizeChange = {},
            onZoomIn = {},
            onZoomOut = {},
            onNodeClicked = {},
            onTakenDownDialogEventConsumed = {},
        )
    }
}
