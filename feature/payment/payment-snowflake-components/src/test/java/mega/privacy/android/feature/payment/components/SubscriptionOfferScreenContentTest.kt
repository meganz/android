package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionOfferScreenContentTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(
        validUntil: Long? = System.currentTimeMillis() / 1000L + 30L * 24L * 3600L,
        validUntilText: String? = "valid until July 11, 2026",
        onBuyClick: () -> Unit = {},
        onDismissClick: () -> Unit = {},
        viewAllPlansText: String? = null,
        onViewAllPlansClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            SubscriptionOfferScreenContent(
                campaignText = "Black Friday: 50% off",
                validUntil = validUntil,
                validUntilText = validUntilText,
                planName = "Pro I",
                priceText = "€4.99/month",
                originalPriceText = "€9.99",
                discountDescriptionText = "Billed at €4.99/month for the first 12 months, €9.99/month after",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = onBuyClick,
                onDismissClick = onDismissClick,
                viewAllPlansText = viewAllPlansText,
                onViewAllPlansClick = onViewAllPlansClick,
            )
        }
    }

    @Test
    fun `test that SubscriptionOfferScreenContent shows header, plan card and buy CTA`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BANNER).assertExists()
        composeRule.onNodeWithTag(
            TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_BADGE,
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_CAMPAIGN)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD).assertExists()
        composeRule.onNodeWithText("Pro I").assertExists()
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).assertExists()
        composeRule.onNodeWithText("Get Pro I").assertExists()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent fades the banner into the page background`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_HEADER_IMAGE_FADE).assertExists()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent hides plan card buy button`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent shows countdown when validUntil is in the future`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN).assertExists()
        composeRule.onNodeWithText("valid until July 11, 2026").assertExists()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent hides countdown when validUntil is null`() {
        setContent(validUntil = null, validUntilText = null)
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN).assertDoesNotExist()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent keeps the countdown at zero when offer has elapsed`() {
        setContent(validUntil = System.currentTimeMillis() / 1000L - 60L)
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN).assertExists()
        composeRule.onAllNodesWithText("00").assertCountEquals(3)
    }

    @Test
    fun `test that SubscriptionOfferScreenContent shows one minute when under a minute is left`() {
        setContent(validUntil = System.currentTimeMillis() / 1000L + 30L)
        composeRule.onAllNodesWithText("00").assertCountEquals(2)
        composeRule.onNodeWithText("01").assertIsDisplayed()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent buy CTA triggers onBuyClick`() {
        var clicks = 0
        setContent(onBuyClick = { clicks++ })
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `test that SubscriptionOfferScreenContent dismiss icon triggers onDismissClick`() {
        var clicks = 0
        setContent(onDismissClick = { clicks++ })
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS).performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun `test that SubscriptionOfferScreenContent hides the view all plans button when text is null`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent shows the view all plans button when text is set`() {
        setContent(viewAllPlansText = "View all plans")
        composeRule.onNodeWithTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("View all plans").assertExists()
    }

    @Test
    fun `test that SubscriptionOfferScreenContent view all plans button triggers onViewAllPlansClick`() {
        var clicks = 0
        setContent(viewAllPlansText = "View all plans", onViewAllPlansClick = { clicks++ })
        composeRule.onNodeWithTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON).performClick()
        assertThat(clicks).isEqualTo(1)
    }
}
