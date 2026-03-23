package mega.privacy.android.feature.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountScreen
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_ADDITIONAL_BENEFITS
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_FEATURE_ROW
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_LAZY_COLUMN
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_MONTHLY_CHIP
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SAVE_UP_TO_BADGE
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SUBSCRIPTION_INFO_DESC
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SUBSCRIPTION_INFO_TITLE
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_TERMS_AND_POLICIES
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_YEARLY_CHIP
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpgradeAccountScreenTest {
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
            InstrumentationRegistry.getInstrumentation().targetContext.getString(sharedR.string.choose_account_screen_maybe_later_button_text)
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

    @Test
    fun `test that buy button is shown when plan is selected`() {
        var clickedSubscription: Subscription? = null
        setContent(
            isUpgradeAccount = true,
            onBuyPlanClick = { clickedSubscription = it }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify button exists (in-app checkout button)
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onInAppCheckoutClick is called when buy button is clicked`() {
        var clickedSubscription: Subscription? = null

        setContent(
            isUpgradeAccount = true,
            onBuyPlanClick = { clickedSubscription = it }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Click in-app checkout button
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .performClick()

        // Verify callback was called with correct subscription
        assert(clickedSubscription != null) { "Callback should be called when in-app button is clicked" }
        assert(clickedSubscription?.accountType == AccountType.PRO_I) { "Subscription account type should be PRO_I" }
    }

    @Test
    fun `test that skeleton is shown when subscriptions list is empty`() {
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = emptyList(),
                isSubscriptionFeatureAvailable = true,
            )
        )

        // Verify skeleton items are displayed (3 items by default)
        (0..2).forEach { index ->
            val skeletonTag = "upgrade_account:pro_plan_card_skeleton$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
                .performScrollToNode(hasTestTag(skeletonTag))
                .assertExists()
        }

        // Verify that actual pro plan cards are not displayed
        composeRule.onNodeWithTag("${TEST_TAG_PRO_PLAN_CARD}0")
            .assertDoesNotExist()
    }

    @Test
    fun `test that skeleton is not shown when subscriptions list has items`() {
        setContent()

        // Verify skeleton items are not displayed
        composeRule.onNodeWithTag("upgrade_account:pro_plan_card_skeleton0")
            .assertDoesNotExist()

        // Verify that actual pro plan cards are displayed
        (0..2).forEach { index ->
            val tag = "${TEST_TAG_PRO_PLAN_CARD}$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
                .performScrollToNode(hasTestTag(tag))
                .assertExists()
        }
    }

    @Test
    fun `test that subscription unavailable banner is shown when isSubscriptionFeatureAvailable is false`() {
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            )
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER))
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that pro plan cards are not shown when isSubscriptionFeatureAvailable is false`() {
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            )
        )

        composeRule.onNodeWithTag("${TEST_TAG_PRO_PLAN_CARD}0")
            .assertDoesNotExist()
    }

    @Test
    fun `test that bottom bar is not shown when isSubscriptionFeatureAvailable is false`() {
        setContent(
            isUpgradeAccount = true,
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            )
        )

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `test that subscription period chips are not shown when isSubscriptionFeatureAvailable is false`() {
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            )
        )

        composeRule.onNodeWithTag(TEST_TAG_MONTHLY_CHIP)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_YEARLY_CHIP)
            .assertDoesNotExist()
    }

    @Test
    fun `test that save up to badge is not shown when isSubscriptionFeatureAvailable is false`() {
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            )
        )

        composeRule.onNodeWithTag(TEST_TAG_SAVE_UP_TO_BADGE)
            .assertDoesNotExist()
    }

    @Test
    fun `test that onSubscriptionUnavailableLearnMoreClick is called when Learn more is clicked`() {
        var learnMoreClicked = false
        setContent(
            uiState = UpgradeAccountState(
                localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
                isSubscriptionFeatureAvailable = false,
            ),
            onSubscriptionUnavailableLearnMoreClick = { learnMoreClicked = true }
        )

        val learnMoreText = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(sharedR.string.general_learn_more)
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_SUBSCRIPTION_UNAVAILABLE_BANNER))
        composeRule.onNodeWithText(learnMoreText)
            .performScrollTo()
            .performClick()

        assert(learnMoreClicked) { "onSubscriptionUnavailableLearnMoreClick should be called when Learn more is clicked" }
    }

    private fun setContent(
        isUpgradeAccount: Boolean = false,
        onBuyPlanClick: (Subscription) -> Unit = {},
        onFreePlanClick: () -> Unit = {},
        maybeLaterClicked: () -> Unit = {},
        onSubscriptionUnavailableLearnMoreClick: () -> Unit = {},
        uiState: UpgradeAccountState = UpgradeAccountState(
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
            isSubscriptionFeatureAvailable = true,
        ),
    ) = composeRule.setContent {
        UpgradeAccountScreen(
            onInAppCheckoutClick = onBuyPlanClick,
            onFreePlanClicked = onFreePlanClick,
            maybeLaterClicked = maybeLaterClicked,
            uiState = uiState,
            onBack = {},
            isUpgradeAccount = isUpgradeAccount,
            onSubscriptionUnavailableLearnMoreClick = onSubscriptionUnavailableLearnMoreClick,
        )
    }
}
