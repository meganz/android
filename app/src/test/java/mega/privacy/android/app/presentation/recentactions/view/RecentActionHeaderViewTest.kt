package mega.privacy.android.app.presentation.recentactions.view

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RecentActionHeaderViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that recent action list item is displayed correctly when all information provided`() {
        val date = "2024-04-04"
        composeRule.setContent {
            RecentActionHeaderView(date)
        }
        composeRule.onNodeWithTag(HEADER_TEST_TAG, true).assertTextEquals(date)
    }
}
