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
class PlanPriceCardTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that PlanPriceCard shows monthly price and benefits`() {
        composeRule.setContent {
            PlanPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                monthlyPriceText = "€4.99/month",
                storageText = "2 TB storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = {},
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PLAN_PRICE_CARD).assertExists()
        composeRule.onNodeWithText("Pro I").assertIsDisplayed()
        composeRule.onNodeWithText("€4.99/month").assertIsDisplayed()
        composeRule.onNodeWithText("2 TB storage").assertIsDisplayed()
        composeRule.onNodeWithText("2 TB transfer").assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON).assertExists()
    }

    @Test
    fun `test that PlanPriceCard shows yearly total and recommended label when recommended`() {
        composeRule.setContent {
            PlanPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro II",
                monthlyPriceText = "€9.99/month",
                yearlyTotalText = "€119.88 charged yearly",
                storageText = "10 TB storage",
                transferText = "10 TB transfer",
                buyButtonText = "Get Pro II",
                onBuyClick = {},
                isRecommended = true,
                recommendedLabel = "Recommended",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PLAN_PRICE_CARD_RECOMMENDED, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("€119.88 charged yearly").assertIsDisplayed()
        composeRule.onNodeWithText("€9.99/month").assertIsDisplayed()
    }

    @Test
    fun `test that PlanPriceCard buy button triggers onBuyClick`() {
        var clicks = 0
        composeRule.setContent {
            PlanPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                monthlyPriceText = "€4.99/month",
                storageText = "2 TB storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = { clicks++ },
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON).performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `test that PlanPriceCard hides buy button when isCurrentPlan is true`() {
        composeRule.setContent {
            PlanPriceCard(
                modifier = Modifier.fillMaxWidth(),
                planName = "Pro I",
                monthlyPriceText = "€4.99/month",
                storageText = "2 TB storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = {},
                isCurrentPlan = true,
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON).assertDoesNotExist()
    }
}
