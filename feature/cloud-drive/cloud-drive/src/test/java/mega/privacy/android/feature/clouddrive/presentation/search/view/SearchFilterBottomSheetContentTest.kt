package mega.privacy.android.feature.clouddrive.presentation.search.view

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterResult
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SearchFilterBottomSheetContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that title is displayed for TYPE filter`() {
        setupComposeContent(filterType = SearchFilterType.TYPE)

        composeRule.onNodeWithTag(SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed for LAST_MODIFIED filter`() {
        setupComposeContent(filterType = SearchFilterType.LAST_MODIFIED)

        composeRule.onNodeWithTag(SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that title is displayed for DATE_ADDED filter`() {
        setupComposeContent(filterType = SearchFilterType.DATE_ADDED)

        composeRule.onNodeWithTag(SEARCH_FILTER_BOTTOM_SHEET_TITLE_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that type filter options are displayed`() {
        setupComposeContent(filterType = SearchFilterType.TYPE)
        composeRule.waitForIdle()

        // Test that at least one option is displayed
        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_${TypeFilterOption.Images}", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that date filter options are displayed for LAST_MODIFIED`() {
        setupComposeContent(filterType = SearchFilterType.LAST_MODIFIED)
        composeRule.waitForIdle()

        // Test that at least one option is displayed
        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_${DateFilterOption.Today}", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that date filter options are displayed for DATE_ADDED`() {
        setupComposeContent(filterType = SearchFilterType.DATE_ADDED)
        composeRule.waitForIdle()

        // Test that at least one option is displayed
        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_${DateFilterOption.Today}", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selecting a type filter option calls callback with correct result`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = TypeFilterOption.Images

        setupComposeContent(
            filterType = SearchFilterType.TYPE,
            selectedTypeFilter = null,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.Type(selectedOption))
    }

    @Test
    fun `test that selecting a date modified filter option calls callback with correct result`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = DateFilterOption.Today

        setupComposeContent(
            filterType = SearchFilterType.LAST_MODIFIED,
            selectedDateModifiedFilter = null,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.DateModified(selectedOption))
    }

    @Test
    fun `test that selecting a date added filter option calls callback with correct result`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = DateFilterOption.Last7Days

        setupComposeContent(
            filterType = SearchFilterType.DATE_ADDED,
            selectedDateAddedFilter = null,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.DateAdded(selectedOption))
    }

    @Test
    fun `test that clicking selected type filter option deselects it`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = TypeFilterOption.Documents

        setupComposeContent(
            filterType = SearchFilterType.TYPE,
            selectedTypeFilter = selectedOption,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.Type(null))
    }

    @Test
    fun `test that clicking selected date modified filter option deselects it`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = DateFilterOption.Last30Days

        setupComposeContent(
            filterType = SearchFilterType.LAST_MODIFIED,
            selectedDateModifiedFilter = selectedOption,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.DateModified(null))
    }

    @Test
    fun `test that clicking selected date added filter option deselects it`() {
        val onFilterSelected = mock<(SearchFilterResult) -> Unit>()
        val selectedOption = DateFilterOption.ThisYear

        setupComposeContent(
            filterType = SearchFilterType.DATE_ADDED,
            selectedDateAddedFilter = selectedOption,
            onFilterSelected = onFilterSelected
        )

        composeRule.onNodeWithTag("${SEARCH_FILTER_OPTION_TAG}_$selectedOption", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()

        verify(onFilterSelected).invoke(SearchFilterResult.DateAdded(null))
    }

    private fun setupComposeContent(
        filterType: SearchFilterType = SearchFilterType.TYPE,
        selectedTypeFilter: TypeFilterOption? = null,
        selectedDateModifiedFilter: DateFilterOption? = null,
        selectedDateAddedFilter: DateFilterOption? = null,
        onFilterSelected: (SearchFilterResult) -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                SearchFilterBottomSheetContent(
                    filterType = filterType,
                    selectedTypeFilter = selectedTypeFilter,
                    selectedDateModifiedFilter = selectedDateModifiedFilter,
                    selectedDateAddedFilter = selectedDateAddedFilter,
                    onFilterSelected = onFilterSelected
                )
            }
        }
    }
}

