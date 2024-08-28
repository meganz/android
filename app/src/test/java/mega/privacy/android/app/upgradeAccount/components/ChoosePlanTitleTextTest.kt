package mega.privacy.android.app.upgradeAccount.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.view.components.ChoosePlanTitleText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
internal class ChoosePlanTitleTextTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that choose plan title is displayed correctly`() {
        composeRule.setContent {
            ChoosePlanTitleText("test:")
        }

        composeRule.onNodeWithTag("test:title").assertIsDisplayed()
        composeRule.onNodeWithText(fromId(R.string.account_upgrade_account_title_choose_right_plan))
            .assertIsDisplayed()
    }
}