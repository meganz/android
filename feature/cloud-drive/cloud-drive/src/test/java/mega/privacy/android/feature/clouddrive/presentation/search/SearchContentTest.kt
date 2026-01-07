package mega.privacy.android.feature.clouddrive.presentation.search

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchUiState
import mega.privacy.android.feature.clouddrive.presentation.search.view.FILTER_CHIPS_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SearchContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that pre-search view is displayed when search has not been performed`() {
        setupComposeContent(
            uiState = SearchUiState()
        )

        composeRule.onNodeWithTag(SEARCH_CONTENT_PRE_SEARCH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_CONTENT_EMPTY_TAG).assertIsNotDisplayed()
        composeRule.onNodeWithTag(SEARCH_CONTENT_RESULTS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that empty view is displayed when search returns no results`() {
        setupComposeContent(
            uiState = SearchUiState(
                searchText = "test",
                searchedQuery = "test",
                nodesLoadingState = NodesLoadingState.FullyLoaded,
            )
        )

        composeRule.onNodeWithTag(SEARCH_CONTENT_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_CONTENT_PRE_SEARCH_TAG).assertIsNotDisplayed()
        composeRule.onNodeWithTag(SEARCH_CONTENT_RESULTS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that filter chips are displayed for cloud drive source type`() {
        setupComposeContent(
            uiState = SearchUiState(
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that filter chips are displayed for home source type`() {
        setupComposeContent(
            uiState = SearchUiState(
                nodeSourceType = NodeSourceType.HOME,
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that filter chips are not displayed for rubbish bin source type`() {
        setupComposeContent(
            uiState = SearchUiState(
                nodeSourceType = NodeSourceType.RUBBISH_BIN,
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that filter chips are not displayed for incoming shares source type`() {
        setupComposeContent(
            uiState = SearchUiState(
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )
        )

        composeRule.onNodeWithTag(FILTER_CHIPS_TAG).assertIsNotDisplayed()
    }

    private fun setupComposeContent(
        uiState: SearchUiState = SearchUiState(),
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                SearchContent(
                    uiState = uiState,
                    contentPadding = PaddingValues(0.dp),
                    isListView = true,
                    spanCount = 2,
                    onFilterClicked = {},
                    onMenuClicked = {},
                    onItemClicked = {},
                    onLongClicked = {},
                    onSortOrderClick = {},
                    onChangeViewTypeClicked = {},
                )
            }
        }
    }
}

