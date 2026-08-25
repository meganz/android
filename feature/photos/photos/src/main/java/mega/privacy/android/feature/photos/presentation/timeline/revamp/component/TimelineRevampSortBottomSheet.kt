package mega.privacy.android.feature.photos.presentation.timeline.revamp.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampSortConfiguration
import mega.privacy.android.feature.photos.presentation.timeline.revamp.TimelineRevampSortOption
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MediaScreenSortByNewestSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenSortByOldestSelectedEvent

/**
 * The Timeline Revamp's sort sheet: Date Added or Date Modified, with the direction toggled by
 * tapping the row that is already selected (the [SortBottomSheet] convention).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelineRevampSortBottomSheet(
    selected: TimelineRevampSortConfiguration,
    onDismissRequest: () -> Unit,
    onSortChange: (TimelineRevampSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
) {
    SortBottomSheet(
        modifier = modifier,
        title = stringResource(sharedR.string.action_sort_by_header),
        options = TimelineRevampSortOption.entries,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        selectedSort = SortBottomSheetResult(
            sortOptionItem = selected.option,
            sortDirection = selected.direction,
        ),
        onDismissRequest = onDismissRequest,
        onSortOptionSelected = { result ->
            result?.let {
                trackSortDirection(it.sortDirection)
                onSortChange(
                    TimelineRevampSortConfiguration(
                        option = it.sortOptionItem,
                        direction = it.sortDirection,
                    )
                )
            }
        },
    )
}

private fun trackSortDirection(direction: SortDirection) {
    val event = when (direction) {
        SortDirection.Descending -> MediaScreenSortByNewestSelectedEvent
        SortDirection.Ascending -> MediaScreenSortByOldestSelectedEvent
    }
    Analytics.tracker.trackEvent(event)
}

@CombinedThemePreviews
@Composable
private fun TimelineRevampSortBottomSheetPreview() {
    AndroidThemeForPreviews {
        TimelineRevampSortBottomSheet(
            selected = TimelineRevampSortConfiguration.Default,
            onDismissRequest = {},
            onSortChange = {},
        )
    }
}
