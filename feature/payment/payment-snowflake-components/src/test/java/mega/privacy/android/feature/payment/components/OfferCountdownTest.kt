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
class OfferCountdownTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setContent() {
        composeRule.setContent {
            OfferCountdown(
                modifier = Modifier.fillMaxWidth(),
                validUntilText = "valid until July 11, 2026",
                days = "05",
                hours = "12",
                minutes = "01",
                daysLabel = "Days",
                hoursLabel = "Hours",
                minutesLabel = "Minutes",
            )
        }
    }

    @Test
    fun `test that OfferCountdown shows the valid until caption`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN).assertExists()
        composeRule.onNodeWithText("valid until July 11, 2026").assertIsDisplayed()
    }

    @Test
    fun `test that OfferCountdown shows the days, hours and minutes values`() {
        setContent()
        composeRule.onNodeWithText("05").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("01").assertIsDisplayed()
    }

    @Test
    fun `test that OfferCountdown shows the unit labels`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN_DAYS).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN_HOURS).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN_MINUTES).assertExists()
        composeRule.onNodeWithText("Days").assertIsDisplayed()
        composeRule.onNodeWithText("Hours").assertIsDisplayed()
        composeRule.onNodeWithText("Minutes").assertIsDisplayed()
    }
}
