package test.mega.privacy.android.app.upgradeAccount.payment.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.payment.UserSubscription
import mega.privacy.android.app.upgradeAccount.payment.component.BillingOptionsView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class BillingOptionsViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the radio button show when isBillingEnable as true`() {
        composeTestRule.setContent {
            BillingOptionsView(
                isMonthly = true,
                isBillingEnable = true,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = composeTestRule.activity.getString(
                    R.string.billed_monthly_text,
                    "99$"
                ),
                yearlyPrice = composeTestRule.activity.getString(
                    R.string.billed_yearly_text,
                    "99$"
                ),
                onSelectChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "monthly_radio").assertIsDisplayed()
        composeTestRule.onNodeWithTag(testTag = "yearly_radio").assertIsDisplayed()
    }

    @Test
    fun `test that the radio button hide when isBillingEnable as false`() {
        composeTestRule.setContent {
            BillingOptionsView(
                isMonthly = true,
                isBillingEnable = false,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = composeTestRule.activity.getString(
                    R.string.billed_monthly_text,
                    "99$"
                ),
                yearlyPrice = composeTestRule.activity.getString(
                    R.string.billed_yearly_text,
                    "99$"
                ),
                onSelectChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "monthly_radio").assertDoesNotExist()
        composeTestRule.onNodeWithTag(testTag = "yearly_radio").assertDoesNotExist()
    }

    @Test
    fun `test that the text billing_period_title visible`() {
        composeTestRule.setContent {
            BillingOptionsView(
                isMonthly = true,
                isBillingEnable = false,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = composeTestRule.activity.getString(
                    R.string.billed_monthly_text,
                    "99$"
                ),
                yearlyPrice = composeTestRule.activity.getString(
                    R.string.billed_yearly_text,
                    "99$"
                ),
                onSelectChange = {}
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.billing_period_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the monthly radio button selected when isMonthly as true`() {
        composeTestRule.setContent {
            BillingOptionsView(
                isMonthly = true,
                isBillingEnable = true,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = composeTestRule.activity.getString(
                    R.string.billed_monthly_text,
                    "99$"
                ),
                yearlyPrice = composeTestRule.activity.getString(
                    R.string.billed_yearly_text,
                    "99$"
                ),
                onSelectChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "monthly_radio").assertIsSelected()
        composeTestRule.onNodeWithTag(testTag = "yearly_radio").assertIsNotSelected()
    }

    @Test
    fun `test that the yearly radio button selected when isMonthly as false`() {
        composeTestRule.setContent {
            BillingOptionsView(
                isMonthly = false,
                isBillingEnable = true,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = composeTestRule.activity.getString(
                    R.string.billed_monthly_text,
                    "99$"
                ),
                yearlyPrice = composeTestRule.activity.getString(
                    R.string.billed_yearly_text,
                    "99$"
                ),
                onSelectChange = {}
            )
        }

        composeTestRule.onNodeWithTag(testTag = "monthly_radio").assertIsNotSelected()
        composeTestRule.onNodeWithTag(testTag = "yearly_radio").assertIsSelected()
    }
}