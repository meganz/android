package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrentPlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that CurrentPlanCard shows plan, cycle and help text`() {
        composeRule.setContent {
            CurrentPlanCard(
                modifier = Modifier.fillMaxWidth(),
                currentPlanLabel = "Current plan",
                planName = "Pro I",
                cycleText = "Yearly subscription",
                helpText = "Renews on 8 July 2027",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_CARD).assertExists()
        composeRule.onNodeWithText("Current plan").assertIsDisplayed()
        composeRule.onNodeWithText("Pro I").assertIsDisplayed()
        composeRule.onNodeWithText("Yearly subscription").assertIsDisplayed()
        composeRule.onNodeWithText("Renews on 8 July 2027").assertIsDisplayed()
    }

    @Test
    fun `test that CurrentPlanCard hides help text when null`() {
        composeRule.setContent {
            CurrentPlanCard(
                modifier = Modifier.fillMaxWidth(),
                currentPlanLabel = "Current plan",
                planName = "Pro I",
                cycleText = "Monthly",
                helpText = null,
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_HELP_TEXT, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that CurrentPlanCard shows expiring badge when expiring label is set`() {
        composeRule.setContent {
            CurrentPlanCard(
                modifier = Modifier.fillMaxWidth(),
                currentPlanLabel = "Current plan",
                planName = "Pro I",
                cycleText = "12 months",
                helpText = "Expires on 8 July 2027",
                expiringLabel = "Expiring",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_EXPIRING_BADGE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Expiring").assertIsDisplayed()
    }

    @Test
    fun `test that CurrentPlanCard hides expiring badge when expiring label is null`() {
        composeRule.setContent {
            CurrentPlanCard(
                modifier = Modifier.fillMaxWidth(),
                currentPlanLabel = "Current plan",
                planName = "Pro I",
                cycleText = "12 months",
                helpText = "Expires on 8 July 2027",
                expiringLabel = null,
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_EXPIRING_BADGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
