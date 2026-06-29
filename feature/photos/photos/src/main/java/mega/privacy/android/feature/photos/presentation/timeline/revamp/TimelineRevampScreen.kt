package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.presentation.component.PhotoNodeBodyV2
import mega.privacy.android.feature.photos.presentation.timeline.component.MediaSkeletonView
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineRevampScreen(
    uiState: TimelineRevampUiState,
    modifier: Modifier = Modifier,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
) {
    when (uiState) {
        is TimelineRevampUiState.Loading -> {
            MediaSkeletonView(
                modifier = modifier.testTag(TIMELINE_REVAMP_LOADING_SKELETON_TAG),
            )
        }

        is TimelineRevampUiState.Empty -> {
            EmptyStateView(
                imagePainter = painterResource(R.drawable.il_glass_image),
                title = stringResource(sharedR.string.timeline_tab_empty_body_no_media_found)
            )
        }

        is TimelineRevampUiState.Data -> {
            TimelineRevampContent(
                sections = uiState.sections,
                sectionStartOffsets = uiState.sectionStartOffsets,
                loadedNodes = uiState.loadedNodes,
                onVisibleRangeChanged = onVisibleRangeChanged,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TimelineRevampContent(
    sections: List<MediaTimelineSection>,
    sectionStartOffsets: List<Int>,
    loadedNodes: Map<Int, PhotosNodeContentItemV2>,
    onVisibleRangeChanged: (firstIndex: Int, lastIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyGridState = rememberLazyGridState()

    NotifyVisibleMediaRange(
        gridState = lazyGridState,
        onVisibleRangeChanged = onVisibleRangeChanged
    )

    FastScrollLazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier
            .fillMaxSize()
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
                MegaText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                        .testTag("$TIMELINE_REVAMP_SECTION_HEADER_TAG${section.groupId}"),
                    text = section.groupId,
                    style = AppTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                )
            }

            items(
                count = section.count.toInt(),
                key = { index -> "$MEDIA_KEY_PREFIX${base + index}" },
            ) { index ->
                val node = loadedNodes[base + index]
                PhotoNodeBodyV2(
                    node = node,
                    modifier = Modifier.padding(all = 1.dp),
                    shouldShowFavourite = node?.isFavourite == true,
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

private const val GRID_COLUMNS = 3
private const val HEADER_KEY_PREFIX = "header_"
private const val MEDIA_KEY_PREFIX = "media_"

internal const val TIMELINE_REVAMP_CONTENT_GRID_TAG = "timeline_revamp_content:grid"
internal const val TIMELINE_REVAMP_SECTION_HEADER_TAG = "timeline_revamp_content:section_header_"
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
            onVisibleRangeChanged = { _, _ -> }
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TimelineRevampEmptyPreview() {
    AndroidThemeForPreviews {
        TimelineRevampScreen(
            uiState = TimelineRevampUiState.Empty,
            onVisibleRangeChanged = { _, _ -> }
        )
    }
}
