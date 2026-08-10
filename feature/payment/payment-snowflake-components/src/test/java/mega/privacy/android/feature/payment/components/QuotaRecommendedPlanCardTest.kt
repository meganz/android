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
class QuotaRecommendedPlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that QuotaRecommendedPlanCard shows badge, name, price, features and usage`() {
        composeRule.setContent {
            QuotaRecommendedPlanCard(
                planName = "Essential",
                monthlyPriceText = "€3.33/month",
                yearlyTotalText = "€40.01 charged yearly",
                storageText = "200 GB storage",
                transferText = "2.4 TB transfer",
                badgeLabel = "Best for you",
                usagePercentage = 10f,
                usageLevel = QuotaUsageLevel.Normal,
                usageText = "Storage: 19 GB out of 200 GB",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_RECOMMENDED_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_RECOMMENDED_BADGE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_RECOMMENDED_PROGRESS).assertExists()
        composeRule.onNodeWithText("Best for you").assertExists()
        composeRule.onNodeWithText("Essential").assertExists()
        composeRule.onNodeWithText("€3.33/month").assertExists()
        composeRule.onNodeWithText("€40.01 charged yearly").assertExists()
        composeRule.onNodeWithText("200 GB storage").assertExists()
        composeRule.onNodeWithText("2.4 TB transfer").assertExists()
        composeRule.onNodeWithText("Storage: 19 GB out of 200 GB").assertExists()
    }

    @Test
    fun `test that QuotaRecommendedPlanCard shows monthly price as primary when no yearly total`() {
        composeRule.setContent {
            QuotaRecommendedPlanCard(
                planName = "Essential",
                monthlyPriceText = "€4.99/month",
                yearlyTotalText = null,
                storageText = "200 GB storage",
                transferText = "2.4 TB transfer",
                badgeLabel = "Best for you",
                usagePercentage = 10f,
                usageLevel = QuotaUsageLevel.Normal,
                usageText = "Storage: 19 GB out of 200 GB",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_RECOMMENDED_PRICE).assertExists()
        composeRule.onNodeWithText("€4.99/month").assertExists()
    }
}
