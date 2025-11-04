package mega.privacy.android.feature.payment

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.model.ChooseAccountState
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.presentation.upgrade.NewChooseAccountScreen
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_ADDITIONAL_BENEFITS
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_FEATURE_ROW
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_LAZY_COLUMN
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SUBSCRIPTION_INFO_DESC
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SUBSCRIPTION_INFO_TITLE
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_TERMS_AND_POLICIES
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewChooseAccountScreenTest {
    private val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    private val formattedSizeMapper = FormattedSizeMapper()

    private val subscriptionProIMonthly = Subscription(
        sku = "pro_i_monthly",
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(8.33F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIYearly = Subscription(
        sku = "pro_i_yearly",
        accountType = AccountType.PRO_I,
        handle = 7472683699866478542,
        storage = 2048,
        transfer = 24576,
        amount = CurrencyAmount(99.96F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIMonthly = Subscription(
        sku = "pro_ii_monthly",
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(16.67F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIYearly = Subscription(
        sku = "pro_ii_yearly",
        accountType = AccountType.PRO_II,
        handle = 370834413380951543,
        storage = 8192,
        transfer = 98304,
        amount = CurrencyAmount(199.99F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIIMonthly = Subscription(
        sku = "pro_iii_monthly",
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(25.00F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIIYearly = Subscription(
        sku = "pro_iii_yearly",
        accountType = AccountType.PRO_III,
        handle = 7225413476571973499,
        storage = 16384,
        transfer = 196608,
        amount = CurrencyAmount(299.99F, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProI = LocalisedSubscription(
        monthlySubscription = subscriptionProIMonthly,
        yearlySubscription = subscriptionProIYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    private val subscriptionProII = LocalisedSubscription(
        monthlySubscription = subscriptionProIIMonthly,
        yearlySubscription = subscriptionProIIYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
    private val subscriptionProIII = LocalisedSubscription(
        monthlySubscription = subscriptionProIIIMonthly,
        yearlySubscription = subscriptionProIIIYearly,
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

        (0..2).forEach { index ->
            val tag = "${TEST_TAG_PRO_PLAN_CARD}$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(tag))
                .assertExists()
            // Assert the plan name text is displayed within the card
            composeRule.onNodeWithTag(tag).performScrollTo().assertExists()
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
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.choose_account_screen_maybe_later_button_text)
        ).assertExists()
    }

    @Test
    fun `test that pro features section is shown correctly`() {
        setContent()
        (0..3).forEach { index ->
            val tag = "${TEST_TAG_FEATURE_ROW}$index"
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
        onBuyPlanClick: (Subscription) -> Unit = {},
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