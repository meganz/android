package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfferPriceCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that OfferPriceCard shows monthly discount price, badge and benefits`() {
        composeRule.setContent {
            OfferPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                priceText = "€4.99/month",
                originalPriceText = "€9.99",
                discountDescriptionText = "Discount price for the first 12 months",
                discountBadgeText = "Black Friday · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I for €4.99/month",
                onBuyClick = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_BADGE, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("Pro I").assertIsDisplayed()
        composeRule.onNodeWithText("€9.99").assertIsDisplayed()
        composeRule.onNodeWithText("€4.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("Discount price for the first 12 months").assertIsDisplayed()
        composeRule.onNodeWithText("2 TB cloud storage").assertIsDisplayed()
        composeRule.onNodeWithText("2 TB transfer").assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON).assertExists()
    }

    @Test
    fun `test that OfferPriceCard shows per-month price and yearly total when monthlyPriceText is set`() {
        composeRule.setContent {
            OfferPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                priceText = "€59.88 charged yearly",
                originalPriceText = "€120",
                discountDescriptionText = "Discount price for the first 12 months",
                discountBadgeText = "Black Friday · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I for €59.88",
                onBuyClick = {},
                monthlyPriceText = "€4.99/month",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_PRICE_PER_MONTH).assertExists()
        composeRule.onNodeWithText("€4.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("€59.88 charged yearly").assertIsDisplayed()
        composeRule.onNodeWithText("€120").assertIsDisplayed()
    }

    @Test
    fun `test that OfferPriceCard hides per-month price when monthlyPriceText is null`() {
        composeRule.setContent {
            OfferPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                priceText = "€4.99/month",
                originalPriceText = "€9.99",
                discountDescriptionText = "Discount price for the first 12 months",
                discountBadgeText = "Black Friday · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I for €4.99/month",
                onBuyClick = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_PRICE_PER_MONTH).assertDoesNotExist()
    }

    @Test
    fun `test that OfferPriceCard buy button triggers onBuyClick`() {
        var clicks = 0
        composeRule.setContent {
            OfferPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                priceText = "€4.99/month",
                originalPriceText = "€9.99",
                discountDescriptionText = "Discount price for the first 12 months",
                discountBadgeText = "Black Friday · 50% off",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I for €4.99/month",
                onBuyClick = { clicks++ },
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON).performClick()
        assertThat(clicks).isEqualTo(1)
    }
}
