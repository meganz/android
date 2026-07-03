package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuotaCurrentPlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that QuotaCurrentPlanCard shows plan name, label and usage text`() {
        composeRule.setContent {
            QuotaCurrentPlanCard(
                planName = "Free",
                currentPlanLabel = "Current plan",
                usagePercentage = 95f,
                usageLevel = QuotaUsageLevel.Warning,
                usageText = "Storage: 19 GB out of 20 GB",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_CURRENT_PLAN_PROGRESS).assertExists()
        composeRule.onNodeWithText("Free").assertExists()
        composeRule.onNodeWithText("Current plan").assertExists()
        composeRule.onNodeWithText("Storage: 19 GB out of 20 GB").assertExists()
    }
}
