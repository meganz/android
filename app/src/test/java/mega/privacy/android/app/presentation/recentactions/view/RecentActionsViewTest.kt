package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.recentactions.model.RecentActionsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentActionsViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that RecentActionsView displays RecentLoadingView when loading`() {
        val uiState = RecentActionsUiState(isLoading = true)
        composeRule.setContent {
            RecentActionsView(uiState, {}, {}, {}, {})
        }

        composeRule.onNodeWithTag(RECENT_LOADING_VIEW_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that RecentActionsView displays RecentActionsHiddenView when hideRecentActivity is true`() {
        val uiState = RecentActionsUiState(isLoading = false, hideRecentActivity = true)
        composeRule.setContent {
            RecentActionsView(uiState, {}, {}, {}, {})
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that RecentActionsView displays RecentActionsEmptyView when loaded and list is empty`() {
        val uiState = RecentActionsUiState(isLoading = false, groupedRecentActionItems = emptyMap())
        composeRule.setContent {
            RecentActionsView(uiState, {}, {}, {}, {})
        }

        composeRule.onNodeWithTag(RECENT_EMPTY_TEXT_TEST_TAG, true).assertExists()
    }
}