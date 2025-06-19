package mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.fromId
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewChooseAccountScreenTest {
    private val localisedPriceStringMapper = LocalisedPriceStringMapper()
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()

    private val subscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(8.33F, Currency("EUR")),
        yearlyAmount = CurrencyAmount(99.96F, Currency("EUR")),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    private val subscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = 8192,
        monthlyTransfer = 8192,
        yearlyTransfer = 98304,
        monthlyAmount = CurrencyAmount(16.67F, Currency("EUR")),
        yearlyAmount = CurrencyAmount(199.99F, Currency("EUR")),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    private val subscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = 16384,
        monthlyTransfer = 16384,
        yearlyTransfer = 196608,
        monthlyAmount = CurrencyAmount(25.00F, Currency("EUR")),
        yearlyAmount = CurrencyAmount(299.99F, Currency("EUR")),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    private val expectedLocalisedSubscriptionsList = listOf(
        subscriptionProI,
        subscriptionProII,
        subscriptionProIII
    )

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that pro plans are shown correctly`() {
        setContent()

        // Check Pro plan cards by tag and title
        val planNames = listOf("Pro I", "Pro II", "Pro III")
        (0..2).forEach { index ->
            val tag = "$TEST_TAG_PRO_PLAN_CARD$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(tag))
                .assertExists()
            // Assert the plan name text is displayed within the card
            composeRule.onNodeWithTag(tag).performScrollTo().assertExists()
            composeRule.onNodeWithText(planNames[index]).assertExists()
        }
        // Check Free plan card by tag
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_FREE_PLAN_CARD))
            .assertExists()
    }

    @Test
    fun `test that top bar is shown correctly with maybe later`() {
        setContent()
        composeRule.onNodeWithText(
            fromId(sharedR.string.choose_account_screen_maybe_later_button_text)
        ).assertExists()
    }

    @Test
    fun `test that pro features section is shown correctly`() {
        setContent()
        (0..3).forEach { index ->
            val tag = "$TEST_TAG_FEATURE_ROW$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(tag))
                .assertExists()
        }
    }

    @Test
    fun `test that additional benefits section is shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_ADDITIONAL_BENEFITS))
            .assertExists()
    }

    @Test
    fun `test that free plan features are shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_FREE_PLAN_CARD))
            .assertExists()
    }

    @Test
    fun `test that subscription info and terms are shown correctly`() {
        setContent()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_SUBSCRIPTION_INFO_TITLE))
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_SUBSCRIPTION_INFO_DESC))
            .assertExists()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_TERMS_AND_POLICIES))
            .assertExists()
    }

    @Test
    fun `test that free plan card is not shown in upgrade account flow`() {
        setContent(isUpgradeAccount = true)
        composeRule.onNodeWithTag(TEST_TAG_FREE_PLAN_CARD).assertDoesNotExist()
    }

    private fun setContent(
        isUpgradeAccount: Boolean = false,
        onBuyPlanClick: (AccountType, Boolean) -> Unit = { _, _ -> },
        onFreePlanClick: () -> Unit = {},
        maybeLaterClicked: () -> Unit = {},
    ) = composeRule.setContent {
        NewChooseAccountScreen(
            onBuyPlanClick = onBuyPlanClick,
            onFreePlanClicked = onFreePlanClick,
            maybeLaterClicked = maybeLaterClicked,
            uiState = ChooseAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
            ),
            onBack = {},
            isUpgradeAccount = isUpgradeAccount
        )
    }
}