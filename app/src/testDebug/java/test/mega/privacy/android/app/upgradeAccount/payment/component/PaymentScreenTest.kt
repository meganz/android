package test.mega.privacy.android.app.upgradeAccount.payment.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.app.upgradeAccount.payment.component.PaymentView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PaymentScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the text payment_methods visible`() {
        composeTestRule.setContent {
            PaymentView(
                isMonthly = true,
                isPaymentMethodAvailable = false,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = "",
                yearlyPrice = "",
                onSelectChange = {},
                titleId = R.string.prolite_account,
                titleColorId = R.color.black
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.payment_methods))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the action bar shows payment text`() {
        composeTestRule.setContent {
            PaymentView(
                isMonthly = true,
                isPaymentMethodAvailable = false,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = "",
                yearlyPrice = "",
                onSelectChange = {},
                titleId = R.string.prolite_account,
                titleColorId = R.color.black
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.payment))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the button hides when isPaymentMethod as false`() {
        composeTestRule.setContent {
            PaymentView(
                isMonthly = true,
                isPaymentMethodAvailable = false,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = "",
                yearlyPrice = "",
                onSelectChange = {},
                titleId = R.string.prolite_account,
                titleColorId = R.color.black
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.proceed))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the title shows when pass the titleId prolite_account`() {
        composeTestRule.setContent {
            PaymentView(
                isMonthly = true,
                isPaymentMethodAvailable = true,
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
                monthlyPrice = "",
                yearlyPrice = "",
                onSelectChange = {},
                titleId = R.string.prolite_account,
                titleColorId = R.color.black
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.prolite_account))
            .assertIsDisplayed()
    }
}