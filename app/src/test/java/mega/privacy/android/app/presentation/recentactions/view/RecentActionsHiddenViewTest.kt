package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentActionsHiddenViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that RecentActionsHiddenView displays image, text, and button correctly`() {
        composeRule.setContent {
            RecentActionsHiddenView()
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_IMAGE_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(RECENTS_HIDDEN_TEXT_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, true).assertExists()
    }
}