package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.view.NewUpgradeAccountView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewUpgradeAccountViewTest {
    private val subscriptionProIMonthly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIMonthly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(19.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIMonthly = Subscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(29.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteMonthly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
    )

    private val expectedSubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that if monthly tab is selected the monthly button displays correctly`() {
        composeRule.setContent {
            NewUpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onTOSClicked = {},
            )
        }

        composeRule.onNode(hasTestTag("Monthly check"), useUnmergedTree = true).assertExists()
        composeRule.onNode(hasTestTag("Yearly check"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that if yearly tab is selected the yearly button displays correctly`() {
        composeRule.setContent {
            NewUpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onTOSClicked = {},
            )
        }
        composeRule.onNodeWithText("Yearly").performClick()

        composeRule.onNode(hasTestTag("Monthly check"), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNode(hasTestTag("Yearly check"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that current subscription label is shown correctly`() {
        composeRule.setContent {
            NewUpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_I, false),
                onBackPressed = {},
                onPlanClicked = {},
                onTOSClicked = {},
            )
        }

        val text = "Current plan"
        val testTag =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro1_account)
        composeRule.onNode(hasTestTag(testTag), useUnmergedTree = true)
            .assert(hasAnyChild(hasTestTag(text)))
    }

    @Test
    fun `test that recommended label is shown correctly`() {
        composeRule.setContent {
            NewUpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_I, false),
                onBackPressed = {},
                onPlanClicked = {},
                onTOSClicked = {},
            )
        }

        val text = "Recommended"
        val testTag =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.pro2_account)
        composeRule.onNode(hasTestTag(testTag), useUnmergedTree = true)
            .assert(hasAnyChild(hasTestTag(text)))
    }

    @Test
    fun `test that buy button shows selected plan correctly`() {
        composeRule.setContent {
            NewUpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onPlanClicked = {},
                onTOSClicked = {},
            )
        }
        val text = "Buy Pro Lite"
        val testTag =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account)
        composeRule.onNodeWithTag(testTag).performClick()

        composeRule.onNodeWithText(text).assertExists()
    }

    private fun getUpgradeAccountState(
        accountType: AccountType,
        showBillingWarning: Boolean,
    ): UpgradeAccountState =
        UpgradeAccountState(
            subscriptionsList = expectedSubscriptionsList,
            currentSubscriptionPlan = accountType,
            showBillingWarning = showBillingWarning,
            currentPayment = UpgradePayment(
                upgradeType = Constants.INVALID_VALUE,
                currentPayment = null
            )
        )
}