package mega.privacy.android.app.presentation.search.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.model.SearchActivityState
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.app.presentation.search.navigation.DATE_ADDED
import mega.privacy.android.app.presentation.search.navigation.DATE_MODIFIED
import mega.privacy.android.app.presentation.search.navigation.TYPE

/**
 * Filter chips view
 *
 * @param state SearchActivityState
 * @param onFilterClicked Function to handle filter click
 * @param updateFilter Function to update filter
 * @param trackAnalytics Function to track analytics
 */
@Composable
fun FilterChipsView(
    state: SearchActivityState,
    onFilterClicked: (String) -> Unit,
    updateFilter: (SearchFilter) -> Unit,
    trackAnalytics: (SearchFilter) -> Unit,
) {
    state.dropdownChipsEnabled?.let { isDropdownChipEnabled ->
        if (isDropdownChipEnabled) {
            DropdownChipToolbar(
                chipItems = listOf(
                    ChipItem(
                        isSelected = state.typeSelectedFilterOption != null,
                        notSelectedTitle = stringResource(id = R.string.search_dropdown_chip_filter_type_file_type),
                        selectedFilterTitle = state.typeSelectedFilterOption?.name
                            ?: stringResource(
                                id = R.string.search_dropdown_chip_filter_type_file_type
                            ),
                        onFilterClicked = { onFilterClicked(TYPE) },
                        testTag = TYPE_DROPDOWN_CHIP_TEST_TAG,
                    ),
                    ChipItem(
                        isSelected = state.dateModifiedSelectedFilterOption != null,
                        notSelectedTitle = stringResource(id = R.string.search_dropdown_chip_filter_type_last_modified),
                        selectedFilterTitle = state.dateModifiedSelectedFilterOption?.name
                            ?: stringResource(
                                id = R.string.search_dropdown_chip_filter_type_last_modified
                            ),
                        onFilterClicked = { onFilterClicked(DATE_MODIFIED) },
                        testTag = DATE_MODIFIED_DROPDOWN_CHIP_TEST_TAG,
                    ),
                    ChipItem(
                        isSelected = state.dateAddedSelectedFilterOption != null,
                        notSelectedTitle = stringResource(id = R.string.search_dropdown_chip_filter_type_date_added),
                        selectedFilterTitle = state.dateAddedSelectedFilterOption?.name
                            ?: stringResource(
                                id = R.string.search_dropdown_chip_filter_type_date_added
                            ),
                        onFilterClicked = { onFilterClicked(DATE_ADDED) },
                        testTag = DATE_ADDED_DROPDOWN_CHIP_TEST_TAG,
                    ),
                ),
                enabled = !state.isSearching,
            )
        } else {
            SearchFilterChipsView(
                filters = state.filters,
                selectedFilter = state.selectedFilter,
                updateFilter = {
                    trackAnalytics(it)
                    updateFilter(it)
                }
            )
        }
    }
}