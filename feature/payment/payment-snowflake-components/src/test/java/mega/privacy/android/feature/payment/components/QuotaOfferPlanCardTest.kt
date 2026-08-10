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
class QuotaOfferPlanCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that QuotaOfferPlanCard shows badge, name, prices, discount, features and usage`() {
        composeRule.setContent {
            QuotaOfferPlanCard(
                planName = "Pro I",
                priceText = "€29.94 charged yearly",
                originalPriceText = "€59.88",
                discountDescriptionText = "Billed at €29.94 for the first year, €119.88 charged yearly after",
                discountBadgeText = "Special offer · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                usagePercentage = 10f,
                usageLevel = QuotaUsageLevel.Normal,
                usageText = "Storage: 19 GB out of 2 TB",
                monthlyPriceText = "€4.99/month",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_OFFER_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_OFFER_BADGE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_OFFER_PRICE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_OFFER_PROGRESS).assertExists()
        composeRule.onNodeWithText("Special offer · 50% off").assertExists()
        composeRule.onNodeWithText("Pro I").assertExists()
        composeRule.onNodeWithText("€4.99/month").assertExists()
        composeRule.onNodeWithText("Billed at €29.94 for the first year, €119.88 charged yearly after")
            .assertExists()
        composeRule.onNodeWithText("2 TB cloud storage").assertExists()
        composeRule.onNodeWithText("2 TB transfer").assertExists()
        composeRule.onNodeWithText("Storage: 19 GB out of 2 TB").assertExists()
    }

    @Test
    fun `test that QuotaOfferPlanCard hides per-month price when not provided`() {
        composeRule.setContent {
            QuotaOfferPlanCard(
                planName = "Pro I",
                priceText = "€4.99/month",
                originalPriceText = "€9.99",
                discountDescriptionText = "Billed at €4.99/month for the first year, €9.99/month after",
                discountBadgeText = "Special offer · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                usagePercentage = 10f,
                usageLevel = QuotaUsageLevel.Normal,
                usageText = "Storage: 19 GB out of 2 TB",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_QUOTA_OFFER_PRICE_PER_MONTH).assertDoesNotExist()
    }
}
