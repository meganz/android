package mega.privacy.android.feature.cloudexplorer.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class ExplorerSearchContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the landing view is shown when there is no query and no recent searches`() {
        setContent(searchUiState = dataState(debouncedQuery = null, recentSearches = emptyList()))

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_LANDING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that recent searches are shown when there is no query and recents exist`() {
        setContent(searchUiState = dataState(debouncedQuery = null, recentSearches = listOf("a", "b")))

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_RECENT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that recent searches are not shown when opted out`() {
        setContent(
            searchUiState = dataState(debouncedQuery = null, recentSearches = listOf("a")),
            recentSearchesEnabled = false,
        )

        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_RECENT_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(EXPLORER_SEARCH_LANDING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the content is shown with the debounced query when a query is entered`() {
        var receivedQuery: String? = null
        setContent(
            query = QUERY,
            searchUiState = dataState(debouncedQuery = QUERY, recentSearches = emptyList()),
            content = { debouncedQuery ->
                receivedQuery = debouncedQuery
                Box(Modifier.testTag(CONTENT_TAG))
            },
        )

        composeTestRule.onNodeWithTag(CONTENT_TAG).assertExists()
        assertThat(receivedQuery).isEqualTo(QUERY)
    }

    private fun setContent(
        query: String? = null,
        searchUiState: ExplorerSearchUiState,
        recentSearchesEnabled: Boolean = true,
        content: @Composable (String?) -> Unit = {},
    ) {
        val searchViewModel = mock<ExplorerSearchViewModel> {
            on { uiState } doReturn MutableStateFlow(searchUiState)
        }
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwnerOf(
                        ExplorerSearchViewModel::class.java to searchViewModel,
                    ),
                ) {
                    ExplorerSearchContent(
                        query = query,
                        onQueryChanged = {},
                        recentSearchesEnabled = recentSearchesEnabled,
                        content = content,
                    )
                }
            }
        }
    }

    private fun dataState(debouncedQuery: String?, recentSearches: List<String>) =
        ExplorerSearchUiState.Data(
            debouncedQuery = debouncedQuery,
            recentSearches = recentSearches,
        )

    private companion object {
        const val QUERY = "report"
        const val CONTENT_TAG = "search_content_marker"
    }
}
