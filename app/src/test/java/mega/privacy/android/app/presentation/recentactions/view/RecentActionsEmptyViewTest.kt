package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentActionsEmptyViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that RecentActionsEmptyView displays image and text correctly`() {
        composeRule.setContent {
            RecentActionsEmptyView()
        }

        composeRule.onNodeWithTag(RECENT_EMPTY_IMAGE_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(RECENT_EMPTY_TEXT_TEST_TAG, true).assertExists()
    }
}