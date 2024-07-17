package mega.privacy.android.app.presentation.search.view

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE
import mega.privacy.mobile.analytics.event.SearchDateAddedDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchFileTypeDropdownChipPressedEvent
import mega.privacy.mobile.analytics.event.SearchLastModifiedDropdownChipPressedEvent

/**
 * Filter chips view
 *
 * @param state SearchActivityState
 * @param onFilterClicked Function to handle filter click
 */
@Composable
fun FilterChipsView(
    state: SearchActivityState,
    onFilterClicked: (String) -> Unit,
) {
    DropdownChipToolbar(
        chipItems = listOf(
            ChipItem(
                isSelected = state.typeSelectedFilterOption != null,
                notSelectedTitle = stringResource(id = SharedR.string.search_dropdown_chip_filter_type_file_type),
                selectedFilterTitle = stringResource(
                    id = state.typeSelectedFilterOption?.title
                        ?: SharedR.string.search_dropdown_chip_filter_type_file_type
                ),
                onFilterClicked = {
                    onFilterClicked(TYPE)
                    Analytics.tracker.trackEvent(SearchFileTypeDropdownChipPressedEvent)
                },
                testTag = TYPE_DROPDOWN_CHIP_TEST_TAG,
            ),
            ChipItem(
                isSelected = state.dateModifiedSelectedFilterOption != null,
                notSelectedTitle = stringResource(id = SharedR.string.search_dropdown_chip_filter_type_last_modified),
                selectedFilterTitle = stringResource(
                    id = state.dateModifiedSelectedFilterOption?.title
                        ?: SharedR.string.search_dropdown_chip_filter_type_last_modified
                ),
                onFilterClicked = {
                    onFilterClicked(DATE_MODIFIED)
                    Analytics.tracker.trackEvent(SearchLastModifiedDropdownChipPressedEvent)
                },
                testTag = DATE_MODIFIED_DROPDOWN_CHIP_TEST_TAG,
            ),
            ChipItem(
                isSelected = state.dateAddedSelectedFilterOption != null,
                notSelectedTitle = stringResource(id = SharedR.string.search_dropdown_chip_filter_type_date_added),
                selectedFilterTitle = stringResource(
                    id = state.dateAddedSelectedFilterOption?.title
                        ?: SharedR.string.search_dropdown_chip_filter_type_date_added
                ),
                onFilterClicked = {
                    onFilterClicked(DATE_ADDED)
                    Analytics.tracker.trackEvent(SearchDateAddedDropdownChipPressedEvent)
                },
                testTag = DATE_ADDED_DROPDOWN_CHIP_TEST_TAG,
            ),
        ),
        enabled = !state.isSearching,
    )
}
