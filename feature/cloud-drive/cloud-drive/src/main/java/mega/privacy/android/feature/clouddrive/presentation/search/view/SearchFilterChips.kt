package mega.privacy.android.feature.clouddrive.presentation.search.view

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.surface.ThemedSurface
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.labelResId
import mega.privacy.android.feature.clouddrive.presentation.search.mapper.titleResId
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.mobile.analytics.event.SearchDateAddedDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedDropdownChipPressedEvent

@Composable
fun SearchFilterChips(
    typeFilterOption: TypeFilterOption?,
    dateModifiedFilterOption: DateFilterOption?,
    dateAddedFilterOption: DateFilterOption?,
    onFilterClicked: (SearchFilterType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ThemedSurface(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(state = rememberScrollState())
                .testTag(FILTER_CHIPS_TAG),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.size(8.dp))

            MegaChip(
                modifier = Modifier
                    .animateContentSize()
                    .testTag(TYPE_FILTER_CHIP_TAG),
                content = stringResource(
                    typeFilterOption?.labelResId ?: SearchFilterType.TYPE.titleResId
                ),
                selected = typeFilterOption != null,
                trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
                onClick = {
                    onFilterClicked(SearchFilterType.TYPE)
                    Analytics.tracker.trackEvent(SearchFileTypeDropdownChipPressedEvent)
                },
                enabled = enabled
            )

            MegaChip(
                modifier = Modifier
                    .animateContentSize()
                    .testTag(DATE_MODIFIED_FILTER_CHIP_TAG),
                content = stringResource(
                    dateModifiedFilterOption?.labelResId ?: SearchFilterType.LAST_MODIFIED.titleResId
                ),
                selected = dateModifiedFilterOption != null,
                trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
                onClick = {
                    onFilterClicked(SearchFilterType.LAST_MODIFIED)
                    Analytics.tracker.trackEvent(SearchLastModifiedDropdownChipPressedEvent)
                },
                enabled = enabled
            )

            MegaChip(
                modifier = Modifier
                    .animateContentSize()
                    .testTag(DATE_ADDED_FILTER_CHIP_TAG),
                content = stringResource(
                    dateAddedFilterOption?.labelResId ?: SearchFilterType.DATE_ADDED.titleResId
                ),
                selected = dateAddedFilterOption != null,
                trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
                onClick = {
                    onFilterClicked(SearchFilterType.DATE_ADDED)
                    Analytics.tracker.trackEvent(SearchDateAddedDropdownChipPressedEvent)
                },
                enabled = enabled
            )

            Spacer(Modifier.size(8.dp))
        }
    }
}


@CombinedThemePreviews
@Composable
private fun SearchFilterChipsPreview() {
    AndroidThemeForPreviews {
        SearchFilterChips(
            typeFilterOption = null,
            dateModifiedFilterOption = DateFilterOption.Last7Days,
            dateAddedFilterOption = null,
            onFilterClicked = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SearchFilterChipsSelectedPreview() {
    AndroidThemeForPreviews {
        SearchFilterChips(
            typeFilterOption = TypeFilterOption.Images,
            dateModifiedFilterOption = DateFilterOption.Today,
            dateAddedFilterOption = DateFilterOption.ThisYear,
            onFilterClicked = {}
        )
    }
}

internal const val FILTER_CHIPS_TAG = "search_filter_chips"
internal const val TYPE_FILTER_CHIP_TAG = "search_filter_chips:type"
internal const val DATE_MODIFIED_FILTER_CHIP_TAG = "search_filter_chips:date_modified"
internal const val DATE_ADDED_FILTER_CHIP_TAG = "search_filter_chips:date_added"
