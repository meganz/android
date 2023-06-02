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
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.UpgradeAccountView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class UpgradeAccountViewTest {
    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()

    private val localisedSubscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = 8192,
        monthlyTransfer = 8192,
        yearlyTransfer = 98304,
        monthlyAmount = CurrencyAmount(19.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            199.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = 16384,
        monthlyTransfer = 16384,
        yearlyTransfer = 196608,
        monthlyAmount = CurrencyAmount(29.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            299.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = 400,
        monthlyTransfer = 1024,
        yearlyTransfer = 12288,
        monthlyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            49.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val expectedLocalisedSubscriptionsList = listOf(
        localisedSubscriptionProLite,
        localisedSubscriptionProI,
        localisedSubscriptionProII,
        localisedSubscriptionProIII
    )

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that if monthly tab is selected the monthly button displays correctly`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onBuyClicked = {},
                onTOSClicked = {},
            )
        }
        composeRule.onNodeWithText("Monthly").performClick()

        composeRule.onNode(hasTestTag("Monthly check"), useUnmergedTree = true).assertExists()
        composeRule.onNode(hasTestTag("Yearly check"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that if yearly tab is selected the yearly button displays correctly`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onBuyClicked = {},
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
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_I, false),
                onBackPressed = {},
                onBuyClicked = {},
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
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_I, false),
                onBackPressed = {},
                onBuyClicked = {},
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
    fun `test that buy button shows pre-selected plan correctly`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.PRO_I, false),
                onBackPressed = {},
                onBuyClicked = {},
                onTOSClicked = {},
            )
        }
        val text = "Buy Pro II"

        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test that buy button shows selected plan correctly if user taps on specific plan`() {
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onBuyClicked = {},
                onTOSClicked = {},
            )
        }
        val text = "Buy Pro Lite"
        val testTag =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account)
        composeRule.onNodeWithTag(testTag).performClick()

        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test that clicking buy button triggers onclick event`() {
        val onBuyClicked = mock<() -> Unit>()
        composeRule.setContent {
            UpgradeAccountView(
                getUpgradeAccountState(AccountType.FREE, false),
                onBackPressed = {},
                onBuyClicked = onBuyClicked,
                onTOSClicked = {},
            )
        }
        val text =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.prolite_account)
        composeRule.onNodeWithText(text).performClick()

        composeRule.onNodeWithTag("BUY_BUTTON_TAG").performClick()

        verify(onBuyClicked).invoke()
    }


    private fun getUpgradeAccountState(
        accountType: AccountType,
        showBillingWarning: Boolean,
    ): UpgradeAccountState =
        UpgradeAccountState(
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
            currentSubscriptionPlan = accountType,
            showBillingWarning = showBillingWarning,
            currentPayment = UpgradePayment(
                upgradeType = Constants.INVALID_VALUE,
                currentPayment = null
            )
        )
}