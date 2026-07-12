package mega.privacy.android.feature.payment

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.components.TEST_TAG_BILLING_PERIOD_MONTHLY
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_CURRENT_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_OFFER_COUNTDOWN
import mega.privacy.android.feature.payment.components.TEST_TAG_OFFER_PRICE_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_OFFER_PRICE_CARD_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_PLAN_PRICE_CARD_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_WHY_GO_PRO_CARD
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountScreen
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_ADDITIONAL_BENEFITS
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_FEATURE_ROW
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_LAZY_COLUMN
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_MONTHLY_CHIP
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_OFFER_HEADER_BADGE
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_REVAMP_PLAN_CARD
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_REVAMP_TITLE
import mega.privacy.android.feature.payment.presentation.upgrade.TEST_TAG_REVAMP_UPGRADE_HINT
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

    private val discount = CurrencyAmount(4.99F, Currency("EUR"))

    private val subscriptionProIOffer = LocalisedSubscription(
        monthlySubscription = subscriptionProIMonthly.copy(
            discountedAmountMonthly = discount,
            discountedPercentage = 50,
            offerPeriod = OfferPeriod.Month(12),
            discountName = "Black Friday",
        ),
        yearlySubscription = subscriptionProIYearly.copy(
            discountedAmountMonthly = discount,
            discountedPercentage = 50,
            offerPeriod = OfferPeriod.Month(12),
            discountName = "Black Friday",
        ),
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val subscriptionProIIOffer = LocalisedSubscription(
        monthlySubscription = subscriptionProIIMonthly.copy(
            discountedAmountMonthly = discount,
            discountedPercentage = 50,
            offerPeriod = OfferPeriod.Month(12),
            discountName = "Black Friday",
        ),
        yearlySubscription = subscriptionProIIYearly.copy(
            discountedAmountMonthly = discount,
            discountedPercentage = 50,
            offerPeriod = OfferPeriod.Month(12),
            discountName = "Black Friday",
        ),
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val singleOfferSubscriptionsList = listOf(
        subscriptionProIOffer,
        subscriptionProII,
        subscriptionProIII,
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

    @Test
    fun `test that revamp content shows title, why-go-pro card and plan cards when flag enabled`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(currentPlan = null),
        )

        composeRule.onNodeWithTag(TEST_TAG_REVAMP_TITLE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_WHY_GO_PRO_CARD))
            .assertExists()
        (0..2).forEach { index ->
            val tag = "$TEST_TAG_REVAMP_PLAN_CARD$index"
            composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
                .performScrollToNode(hasTestTag(tag))
                .assertExists()
        }
    }

    @Test
    fun `test that revamp current plan card is shown for upgrade account`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_CURRENT_PLAN_CARD))
        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_CARD)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that revamp current plan card is not shown for free account`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(currentPlan = AccountType.FREE),
        )

        composeRule.onNodeWithTag(TEST_TAG_CURRENT_PLAN_CARD).assertDoesNotExist()
    }

    @Test
    fun `test that revamp shows upgrade-on-web hint when current plan is the highest plan`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(currentPlan = AccountType.PRO_III),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_REVAMP_UPGRADE_HINT))
        composeRule.onNodeWithTag(TEST_TAG_REVAMP_UPGRADE_HINT)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that revamp hides upgrade-on-web hint when current plan is not the highest plan`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(currentPlan = AccountType.PRO_I),
        )

        composeRule.onNodeWithTag(TEST_TAG_REVAMP_UPGRADE_HINT).assertDoesNotExist()
    }

    @Test
    fun `test that revamp legacy bottom bar buy button is not shown`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that revamp plan card buy button calls onInAppCheckoutClick`() {
        var clickedSubscription: Subscription? = null
        setContent(
            isUpgradeAccount = false,
            isSubscriptionRevampEnabled = true,
            onBuyPlanClick = { clickedSubscription = it },
            uiState = revampUiState(currentPlan = null),
        )

        val firstCardTag = "${TEST_TAG_REVAMP_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(firstCardTag))
        composeRule.onAllNodesWithTag(TEST_TAG_PLAN_PRICE_CARD_BUTTON).onFirst().performClick()

        assert(clickedSubscription != null) {
            "onInAppCheckoutClick should be called when a plan card buy button is clicked"
        }
        assert(clickedSubscription?.accountType == AccountType.PRO_I) {
            "First plan card should buy PRO_I"
        }
    }

    @Test
    fun `test that current plan is excluded from segment when subscription is recurring`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(
                currentPlan = AccountType.PRO_I,
                subscriptionCycle = AccountSubscriptionCycle.YEARLY,
                subscriptionStatus = SubscriptionStatus.VALID,
            ),
        )

        // Scroll past the plan cards so a third card would be composed if it existed.
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_ADDITIONAL_BENEFITS))
        composeRule.onNodeWithTag("${TEST_TAG_REVAMP_PLAN_CARD}1").assertExists()
        composeRule.onNodeWithTag("${TEST_TAG_REVAMP_PLAN_CARD}2").assertDoesNotExist()
    }

    @Test
    fun `test that current plan is shown in segment when subscription is one-off`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(
                currentPlan = AccountType.PRO_I,
                subscriptionCycle = AccountSubscriptionCycle.YEARLY,
                subscriptionStatus = SubscriptionStatus.NONE,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag("${TEST_TAG_REVAMP_PLAN_CARD}2"))
            .assertExists()
    }

    @Test
    fun `test that current plan is excluded from monthly tab when subscription is recurring monthly`() {
        setContent(
            isUpgradeAccount = true,
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(
                currentPlan = AccountType.PRO_I,
                subscriptionCycle = AccountSubscriptionCycle.MONTHLY,
                subscriptionStatus = SubscriptionStatus.VALID,
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_BILLING_PERIOD_MONTHLY))
        composeRule.onNodeWithTag(TEST_TAG_BILLING_PERIOD_MONTHLY).performClick()

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_ADDITIONAL_BENEFITS))
        composeRule.onNodeWithTag("${TEST_TAG_REVAMP_PLAN_CARD}1").assertExists()
        composeRule.onNodeWithTag("${TEST_TAG_REVAMP_PLAN_CARD}2").assertDoesNotExist()
    }

    @Test
    fun `test that single offer shows offer header and price card when flag enabled`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = offerUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_OFFER_HEADER_BADGE).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_OFFER_PRICE_CARD))
            .assertExists()
    }

    @Test
    fun `test that single offer content does not show the revamp title`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = offerUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_REVAMP_TITLE).assertDoesNotExist()
    }

    @Test
    fun `test that offer countdown is not shown when offerValidUntil is null`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = offerUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_OFFER_COUNTDOWN).assertDoesNotExist()
    }

    @Test
    fun `test that remaining plans are shown excluding the featured offer plan`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = offerUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag("${TEST_TAG_REVAMP_PLAN_CARD}1"))
            .assertExists()
        composeRule.onNodeWithTag("${TEST_TAG_REVAMP_PLAN_CARD}2").assertDoesNotExist()
    }

    @Test
    fun `test that offer price card buy button calls onInAppCheckoutClick with the discounted plan`() {
        var clickedSubscription: Subscription? = null
        setContent(
            isSubscriptionRevampEnabled = true,
            onBuyPlanClick = { clickedSubscription = it },
            uiState = offerUiState(),
        )

        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN)
            .performScrollToNode(hasTestTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON))
        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD_BUTTON).performClick()

        assert(clickedSubscription?.accountType == AccountType.PRO_I) {
            "Offer buy button should purchase the discounted PRO_I plan"
        }
    }

    @Test
    fun `test that offer highlight falls back to revamp when there is no discount`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = revampUiState(currentPlan = null),
        )

        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_REVAMP_TITLE).assertExists()
    }

    @Test
    fun `test that offer highlight falls back to revamp when there are multiple offers`() {
        setContent(
            isSubscriptionRevampEnabled = true,
            uiState = offerUiState(
                subscriptions = listOf(
                    subscriptionProIOffer,
                    subscriptionProIIOffer,
                    subscriptionProIII,
                ),
            ),
        )

        composeRule.onNodeWithTag(TEST_TAG_OFFER_PRICE_CARD).assertDoesNotExist()
        composeRule.onNodeWithTag(TEST_TAG_REVAMP_TITLE).assertExists()
    }

    private fun offerUiState(
        subscriptions: List<LocalisedSubscription> = singleOfferSubscriptionsList,
    ) = UpgradeAccountState(
        localisedSubscriptionsList = subscriptions,
        isSubscriptionFeatureAvailable = true,
        cheapestSubscriptionAvailable = subscriptionProII,
    )

    private fun revampUiState(
        currentPlan: AccountType? = AccountType.PRO_I,
        subscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.YEARLY,
        subscriptionStatus: SubscriptionStatus = SubscriptionStatus.VALID,
    ) = UpgradeAccountState(
        localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
        isSubscriptionFeatureAvailable = true,
        cheapestSubscriptionAvailable = subscriptionProII,
        currentSubscriptionPlan = currentPlan,
        subscriptionCycle = subscriptionCycle,
        subscriptionStatus = subscriptionStatus,
        subscriptionRenewTime = 1_815_000_000L,
        proExpirationTime = 1_815_000_000L,
    )

    private fun setContent(
        isUpgradeAccount: Boolean = false,
        isSubscriptionRevampEnabled: Boolean = false,
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
            isSubscriptionRevampEnabled = isSubscriptionRevampEnabled,
            onSubscriptionUnavailableLearnMoreClick = onSubscriptionUnavailableLearnMoreClick,
        )
    }
}
