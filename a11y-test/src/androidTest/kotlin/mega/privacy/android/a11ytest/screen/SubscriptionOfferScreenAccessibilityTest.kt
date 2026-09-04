package mega.privacy.android.a11ytest.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import mega.privacy.android.feature.payment.components.SubscriptionOfferScreenContent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Accessibility sweep of the subscription offer landing screen from
 * `:feature:payment:payment-snowflake-components`.
 *
 * The module is UI-only, so the screen takes plain formatted strings rather than a UI-state
 * object and mounts with no feature-module dependencies at all.
 */
@RunWith(AndroidJUnit4::class)
class SubscriptionOfferScreenAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter =
        AccessibilityCheckReporter(reportName = "SubscriptionOfferScreenAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_the_running_offer_screen_passes_accessibility_checks() {
        val validUntil = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) +
            TimeUnit.DAYS.toSeconds(3)
        composeTestRule.setScreenContent {
            SubscriptionOfferScreenContent(
                campaignText = "Black Friday: 50% off",
                validUntil = validUntil,
                validUntilText = "Valid until 6 September 2026",
                planName = "Pro I",
                priceText = "4.99 EUR/month",
                originalPriceText = "9.99 EUR",
                discountDescriptionText = "Billed at 4.99 EUR/month for the first 12 months, " +
                    "9.99 EUR/month after",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = {},
                onDismissClick = {},
                monthlyPriceText = "4.99 EUR/month",
                viewAllPlansText = "View all plans",
                onViewAllPlansClick = {},
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_the_expired_offer_screen_passes_accessibility_checks() {
        composeTestRule.setScreenContent {
            SubscriptionOfferScreenContent(
                campaignText = "Black Friday: 50% off",
                validUntil = null,
                validUntilText = null,
                planName = "Pro I",
                priceText = "9.99 EUR/month",
                originalPriceText = "9.99 EUR",
                discountDescriptionText = "Billed at 9.99 EUR/month",
                storageText = "2 TB cloud storage",
                transferText = "2 TB transfer",
                buyButtonText = "Get Pro I",
                onBuyClick = {},
                onDismissClick = {},
                useBrandButton = false,
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
