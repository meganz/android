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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class OfferBannerTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent(
        validUntil: Long = System.currentTimeMillis() / 1000L +
                28L * 24L * 3600L + 12L * 3600L + 150L,
        onActionClick: () -> Unit = {},
        onDismissClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            OfferBanner(
                title = "Black Friday · Get 50% off",
                subtitle = "€4.99/month for Pro I",
                validUntil = validUntil,
                actionButtonText = "Grab deal",
                onActionClick = onActionClick,
                onDismissClick = onDismissClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    @Test
    fun `test that OfferBanner shows the title and subtitle`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER).assertExists()
        composeRule.onNodeWithText("Black Friday · Get 50% off").assertIsDisplayed()
        composeRule.onNodeWithText("€4.99/month for Pro I").assertIsDisplayed()
    }

    @Test
    fun `test that OfferBanner shows the countdown values and labels when time remains`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_DAYS).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_HOURS).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_MINUTES).assertExists()
        composeRule.onNodeWithText("28").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("02").assertIsDisplayed()
        composeRule.onNodeWithText("Days").assertIsDisplayed()
        composeRule.onNodeWithText("Hours").assertIsDisplayed()
        composeRule.onNodeWithText("Minutes").assertIsDisplayed()
    }

    @Test
    fun `test that OfferBanner hides the whole banner when the offer has elapsed`() {
        setContent(validUntil = System.currentTimeMillis() / 1000L - 60L)
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER).assertDoesNotExist()
        composeRule.onNodeWithText("Black Friday · Get 50% off").assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_DAYS).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_HOURS).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_MINUTES).assertDoesNotExist()
    }

    @Test
    fun `test that OfferBanner shows the banner without a countdown when the offer has no expiry`() {
        setContent(validUntil = 0L)
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER).assertExists()
        composeRule.onNodeWithText("Black Friday · Get 50% off").assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_DAYS).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_HOURS).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_MINUTES).assertDoesNotExist()
    }

    @Test
    fun `test that OfferBanner invokes onActionClick when the action button is tapped`() {
        val onActionClick = mock<() -> Unit>()
        setContent(onActionClick = onActionClick)
        composeRule.onNodeWithText("Grab deal").performClick()
        verify(onActionClick).invoke()
    }

    @Test
    fun `test that OfferBanner invokes onDismissClick when the dismiss icon is tapped`() {
        val onDismissClick = mock<() -> Unit>()
        setContent(onDismissClick = onDismissClick)
        composeRule.onNodeWithTag(TEST_TAG_OFFER_BANNER_DISMISS).performClick()
        verify(onDismissClick).invoke()
    }
}
