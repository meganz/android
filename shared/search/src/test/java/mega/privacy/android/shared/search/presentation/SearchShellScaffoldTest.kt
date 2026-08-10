package mega.privacy.android.shared.search.presentation

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.search.presentation.component.FILTER_CHIPS_TAG
import mega.privacy.android.shared.search.presentation.model.SearchEmptyContent
import mega.privacy.android.shared.search.presentation.model.SearchFilterChipState
import mega.privacy.android.shared.search.presentation.model.SearchShellState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SearchShellScaffoldTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val emptyContent = SearchEmptyContent(
        title = LocalizedText.Literal("Empty"),
        description = LocalizedText.Literal("No results"),
        image = mega.privacy.android.icon.pack.R.drawable.ic_search_02,
    )

    @Test
    fun `test that recent searches are displayed before searching when there are recent searches`() {
        setupContent(
            SearchShellState(
                isPreSearch = true,
                recentSearches = RECENT_SEARCHES,
                isRecentSearchesLoading = false,
            )
        )

        composeRule.onNodeWithTag(SEARCH_SHELL_RECENT_SEARCHES_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_SHELL_RESULTS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that landing is displayed before searching when there are no recent searches`() {
        setupContent(
            SearchShellState(
                isPreSearch = true,
                recentSearches = emptyList(),
                isRecentSearchesLoading = false,
            )
        )

        composeRule.onNodeWithTag(SEARCH_SHELL_LANDING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_SHELL_RESULTS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that empty view is displayed when search returns no results`() {
        setupContent(
            SearchShellState(
                isPreSearch = false,
                isEmpty = true,
            )
        )

        composeRule.onNodeWithTag(SEARCH_SHELL_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_SHELL_RESULTS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that results are displayed when search returns results`() {
        setupContent(
            SearchShellState(
                isPreSearch = false,
                isEmpty = false,
                isLoading = false,
            )
        )

        composeRule.onNodeWithTag(SEARCH_SHELL_RESULTS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that filter chips are displayed when filters are provided`() {
        setupContent(
            SearchShellState(
                isPreSearch = false,
                filters = listOf(
                    SearchFilterChipState(
                        FILTER_ID,
                        LocalizedText.Literal("Type"),
                        isSelected = false
                    ),
                ),
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that filter chips are not displayed when there are no filters`() {
        setupContent(
            SearchShellState(
                isPreSearch = false,
                filters = emptyList(),
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsNotDisplayed()
    }

    private fun setupContent(state: SearchShellState) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                SearchShellScaffold(
                    state = state,
                    landingContent = emptyContent,
                    emptyContent = emptyContent,
                    onSearchTextChange = {},
                    onBack = {},
                    onRecentSearchSelected = {},
                    onClearRecentSearches = {},
                    resultsContent = { ResultsPlaceholder() },
                )
            }
        }
    }

    @Composable
    private fun ResultsPlaceholder() {
        Box(modifier = Modifier.fillMaxSize())
    }

    private companion object {
        const val FILTER_ID = "type"
        val RECENT_SEARCHES = listOf("query1", "query2")
    }
}
