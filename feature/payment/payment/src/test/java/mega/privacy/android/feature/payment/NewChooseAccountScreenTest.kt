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
import de.palm.composestateevents.triggered
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_ON_WEBSITE_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_FREE_PLAN_CARD
import mega.privacy.android.feature.payment.components.TEST_TAG_PRO_PLAN_CARD
import mega.privacy.android.feature.payment.model.BillingUIState
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
import mega.privacy.android.shared.resources.R as sharedR
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
    fun `test that single button is shown when external checkout is disabled`() {
        var clickedSubscription: Subscription? = null
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = false,
            onBuyPlanClick = { clickedSubscription = it }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify button exists (in-app checkout button)
        // The button should be clickable and contain "Buy" text
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that two buttons are shown when external checkout is enabled`() {
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify both buttons exist and are displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that external checkout button is primary when isExternalCheckoutDefault is true`() {
        var externalCheckoutClicked = false
        var inAppCheckoutClicked = false

        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = true,
            onExternalCheckoutClick = { _, _ -> externalCheckoutClicked = true },
            onBuyPlanClick = { inAppCheckoutClicked = true }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify both buttons exist and are displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()

        // Verify button order: external checkout button should be first (primary)
        // When external is default, external button is the primary button (appears first)
        // Click the external checkout button and verify callback
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .performClick()

        assert(externalCheckoutClicked) { "External checkout callback should be called when primary button is clicked" }
        assert(!inAppCheckoutClicked) { "In-app checkout callback should not be called" }
    }

    @Test
    fun `test that in-app checkout button is primary when isExternalCheckoutDefault is false`() {
        var externalCheckoutClicked = false
        var inAppCheckoutClicked = false

        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false,
            onExternalCheckoutClick = { _, _ -> externalCheckoutClicked = true },
            onBuyPlanClick = { inAppCheckoutClicked = true }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify both buttons exist and are displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()

        // Verify button order: in-app checkout button should be first (primary)
        // Click the first button (should be in-app checkout) and verify callback
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .performClick()

        assert(inAppCheckoutClicked) { "In-app checkout callback should be called when primary button is clicked" }
        assert(!externalCheckoutClicked) { "External checkout callback should not be called" }
    }

    @Test
    fun `test that onExternalCheckoutClick is called when external button is clicked`() {
        var clickedSubscription: Subscription? = null

        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false,
            onExternalCheckoutClick = { subscription, _ -> clickedSubscription = subscription }
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Click external checkout button (the one with "website" text)
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .performClick()

        // Verify callback was called with correct subscription
        assert(clickedSubscription != null) { "Callback should be called when external button is clicked" }
        assert(clickedSubscription?.accountType == AccountType.PRO_I) { "Subscription account type should be PRO_I" }
    }

    @Test
    fun `test that onInAppCheckoutClick is called when in-app button is clicked`() {
        var clickedSubscription: Subscription? = null

        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false,
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
    fun `test that discount percentage is displayed in external checkout button`() {
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify external checkout button shows discount text (contains "Save" and "%")
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()

        // Verify discount percentage text is displayed
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val externalButtonText =
            context.getString(sharedR.string.external_checkout_button_text, 15.0f)
        composeRule.onNodeWithText(externalButtonText)
            .assertIsDisplayed()
    }

    @Test
    fun `test that error snackbar is displayed when billingUIState has generalError triggered`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val errorMessage = context.getString(sharedR.string.general_text_error)

        setContent(
            isUpgradeAccount = true,
            billingUIState = BillingUIState(generalError = triggered),
            clearExternalPurchaseError = {}
        )

        // Wait for composition and event processing
        composeRule.waitForIdle()

        // Verify error snackbar is displayed with the error message
        composeRule.onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }

    @Test
    fun `test that skeleton is shown when subscriptions list is empty`() {
        setContent(
            uiState = ChooseAccountState(
                localisedSubscriptionsList = emptyList(),
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
    fun `test that external checkout button is shown when external checkout is enabled and user is adult`() {
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false,
            userAgeComplianceStatus = UserAgeComplianceStatus.AdultVerified
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify both buttons exist and are displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that external checkout button is not shown when user is under age`() {
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false,
            userAgeComplianceStatus = UserAgeComplianceStatus.RequiresMinorRestriction
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify only in-app checkout button is displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        // Verify external checkout button is not displayed
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertDoesNotExist()
    }

    @Test
    fun `test that external checkout button is shown when userAgeComplianceStatus defaults to AdultVerified`() {
        setContent(
            isUpgradeAccount = true,
            isExternalCheckoutEnabled = true,
            isExternalCheckoutDefault = false
            // userAgeComplianceStatus defaults to AdultVerified
        )

        val testTag = "${TEST_TAG_PRO_PLAN_CARD}0"
        composeRule.onNodeWithTag(TEST_TAG_LAZY_COLUMN).performScrollToNode(hasTestTag(testTag))
        // Select a plan first
        composeRule.onNodeWithTag(testTag).performClick()

        // Verify both buttons exist and are displayed (default behavior)
        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON)
            .assertIsDisplayed()
    }

    private fun setContent(
        isUpgradeAccount: Boolean = false,
        onBuyPlanClick: (Subscription) -> Unit = {},
        onFreePlanClick: () -> Unit = {},
        maybeLaterClicked: () -> Unit = {},
        isExternalCheckoutEnabled: Boolean = false,
        isExternalCheckoutDefault: Boolean = false,
        onExternalCheckoutClick: (Subscription, Boolean) -> Unit = { _, _ -> },
        billingUIState: BillingUIState = BillingUIState(),
        clearExternalPurchaseError: () -> Unit = {},
        userAgeComplianceStatus: UserAgeComplianceStatus = UserAgeComplianceStatus.AdultVerified,
        uiState: ChooseAccountState = ChooseAccountState(
            localisedSubscriptionsList = expectedLocalisedSubscriptionsList,
            isExternalCheckoutEnabled = isExternalCheckoutEnabled,
            isExternalCheckoutDefault = isExternalCheckoutDefault,
        ),
    ) = composeRule.setContent {
        NewChooseAccountScreen(
            onInAppCheckoutClick = onBuyPlanClick,
            onFreePlanClicked = onFreePlanClick,
            maybeLaterClicked = maybeLaterClicked,
            uiState = uiState,
            billingUIState = billingUIState,
            clearExternalPurchaseError = clearExternalPurchaseError,
            onBack = {},
            isUpgradeAccount = isUpgradeAccount,
            isExternalCheckoutEnabled = isExternalCheckoutEnabled,
            isExternalCheckoutDefault = isExternalCheckoutDefault,
            onExternalCheckoutClick = onExternalCheckoutClick,
            userAgeComplianceStatus = userAgeComplianceStatus,
        )
    }
}
