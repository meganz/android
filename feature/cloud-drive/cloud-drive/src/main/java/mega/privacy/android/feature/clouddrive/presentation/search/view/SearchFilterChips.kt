package mega.privacy.android.feature.clouddrive.presentation.search.view

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
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.SearchDateAddedDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedDropdownChipPressedEvent

@Composable
fun SearchFilterChips(
    typeFilterOption: TypeFilterOption?,
    dateModifiedFilterOption: DateFilterOption?,
    dateAddedFilterOption: DateFilterOption?,
    onFilterClicked: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .horizontalScroll(state = rememberScrollState())
            .testTag(FILTER_CHIPS_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.size(8.dp))

        MegaChip(
            modifier = Modifier.testTag(TYPE_FILTER_CHIP_TAG),
            content = typeFilterOption?.toDisplayString()
                ?: stringResource(sharedR.string.search_dropdown_chip_filter_type_file_type),
            selected = typeFilterOption != null,
            trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
            onClick = {
                onFilterClicked(FilterType.TYPE)
                Analytics.tracker.trackEvent(SearchFileTypeDropdownChipPressedEvent)
            },
            enabled = enabled
        )

        MegaChip(
            modifier = Modifier.testTag(DATE_MODIFIED_FILTER_CHIP_TAG),
            content = dateModifiedFilterOption?.toDisplayString()
                ?: stringResource(sharedR.string.search_dropdown_chip_filter_type_last_modified),
            selected = dateModifiedFilterOption != null,
            trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
            onClick = {
                onFilterClicked(FilterType.LAST_MODIFIED)
                Analytics.tracker.trackEvent(SearchLastModifiedDropdownChipPressedEvent)
            },
            enabled = enabled
        )

        MegaChip(
            modifier = Modifier.testTag(DATE_ADDED_FILTER_CHIP_TAG),
            content = dateAddedFilterOption?.toDisplayString()
                ?: stringResource(sharedR.string.search_dropdown_chip_filter_type_date_added),
            selected = dateAddedFilterOption != null,
            trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
            onClick = {
                onFilterClicked(FilterType.DATE_ADDED)
                Analytics.tracker.trackEvent(SearchDateAddedDropdownChipPressedEvent)
            },
            enabled = enabled
        )

        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun TypeFilterOption.toDisplayString(): String = stringResource(
    when (this) {
        TypeFilterOption.Audio -> sharedR.string.search_dropdown_chip_filter_type_file_type_audio
        TypeFilterOption.Video -> sharedR.string.search_dropdown_chip_filter_type_file_type_video
        TypeFilterOption.Images -> sharedR.string.search_dropdown_chip_filter_type_file_type_images
        TypeFilterOption.Documents -> sharedR.string.search_dropdown_chip_filter_type_file_type_documents
        TypeFilterOption.Folder -> sharedR.string.search_dropdown_chip_filter_type_file_type_folders
        TypeFilterOption.Pdf -> sharedR.string.search_dropdown_chip_filter_type_file_type_pdf
        TypeFilterOption.Presentation -> sharedR.string.search_dropdown_chip_filter_type_file_type_presentations
        TypeFilterOption.Spreadsheet -> sharedR.string.search_dropdown_chip_filter_type_file_type_spreadsheets
        TypeFilterOption.Other -> sharedR.string.search_dropdown_chip_filter_type_file_type_others
    }
)

@Composable
private fun DateFilterOption.toDisplayString(): String = stringResource(
    when (this) {
        DateFilterOption.Today -> sharedR.string.search_dropdown_chip_filter_type_date_today
        DateFilterOption.Last7Days -> sharedR.string.search_dropdown_chip_filter_type_date_last_seven_days
        DateFilterOption.Last30Days -> sharedR.string.search_dropdown_chip_filter_type_date_last_thirty_days
        DateFilterOption.ThisYear -> sharedR.string.search_dropdown_chip_filter_type_date_this_year
        DateFilterOption.LastYear -> sharedR.string.search_dropdown_chip_filter_type_date_last_year
        DateFilterOption.Older -> sharedR.string.search_dropdown_chip_filter_type_date_older
    }
)

enum class FilterType {
    TYPE,
    LAST_MODIFIED,
    DATE_ADDED,
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
