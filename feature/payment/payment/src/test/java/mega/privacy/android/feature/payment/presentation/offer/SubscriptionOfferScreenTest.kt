package mega.privacy.android.feature.payment.presentation.offer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.OfferPeriod
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON
import mega.privacy.android.feature.payment.components.TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS
import mega.privacy.android.feature.payment.components.TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_ERROR
import mega.privacy.android.feature.payment.components.TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_RETRY
import mega.privacy.android.feature.payment.components.TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_SKELETON
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionOfferScreenTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        state: SubscriptionOfferState,
        onRetryClick: () -> Unit = {},
        onDismiss: () -> Unit = {},
        onBuyClick: (Subscription) -> Unit = {},
        onViewAllPlansClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            SubscriptionOfferScreen(
                uiState = state,
                onBuyClick = onBuyClick,
                onDismiss = onDismiss,
                onViewAllPlansClick = onViewAllPlansClick,
                onRetryClick = onRetryClick,
            )
        }
    }

    private val offerSubscription = LocalisedSubscription(
        monthlySubscription = Subscription(
            sku = "pro_i_monthly",
            accountType = AccountType.PRO_I,
            handle = 370834413380951543,
            storage = 2048,
            transfer = 2048,
            amount = CurrencyAmount(9.99F, Currency("EUR")),
            discountedAmountMonthly = CurrencyAmount(4.99F, Currency("EUR")),
            discountedPercentage = 50,
            discountName = "Black Friday",
            offerPeriod = OfferPeriod.Month(12),
        ),
        yearlySubscription = null,
        localisedPriceCurrencyCode = LocalisedPriceCurrencyCodeStringMapper(),
        formattedSize = FormattedSizeMapper(),
    )

    private fun loadedState(validUntil: Long) = SubscriptionOfferState(
        isLoading = false,
        offerSubscription = offerSubscription,
        isMonthly = true,
        offerValidUntil = validUntil,
    )

    @Test
    fun `test that loading state shows the skeleton`() {
        setScreen(SubscriptionOfferState(isLoading = true))

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_SKELETON).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_ERROR).assertDoesNotExist()
    }

    @Test
    fun `test that the skeleton keeps the dismiss affordance available`() {
        var dismissed = false
        setScreen(
            state = SubscriptionOfferState(isLoading = true),
            onDismiss = { dismissed = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS).performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `test that no connection state shows the error message`() {
        setScreen(SubscriptionOfferState(isLoading = false, isConnected = false))

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_ERROR).assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(sharedR.string.subscription_quota_no_connection_title)
        ).assertExists()
        composeRule.onNodeWithText(
            composeRule.activity.getString(
                sharedR.string.subscription_offer_no_connection_description
            )
        ).assertExists()
    }

    @Test
    fun `test that no connection state takes precedence over the loading state`() {
        setScreen(SubscriptionOfferState(isLoading = true, isConnected = false))

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_ERROR).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_SKELETON).assertDoesNotExist()
    }

    @Test
    fun `test that load error state shows the error message while connected`() {
        setScreen(
            SubscriptionOfferState(isLoading = false, isConnected = true, hasLoadError = true)
        )

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_ERROR).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_SKELETON).assertDoesNotExist()
    }

    @Test
    fun `test that try again click invokes the retry callback`() {
        var retried = false
        setScreen(
            state = SubscriptionOfferState(isLoading = false, isConnected = false),
            onRetryClick = { retried = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_RETRY).performClick()

        assertThat(retried).isTrue()
    }

    @Test
    fun `test that buy CTA starts the purchase while the offer is running`() {
        var bought: Subscription? = null
        var viewedAllPlans = false
        setScreen(
            state = loadedState(validUntil = System.currentTimeMillis() / 1000L + 3600L),
            onBuyClick = { bought = it },
            onViewAllPlansClick = { viewedAllPlans = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).performClick()

        assertThat(bought).isNotNull()
        assertThat(viewedAllPlans).isFalse()
    }

    @Test
    fun `test that the CTA becomes view all plans when the offer has expired`() {
        setScreen(state = loadedState(validUntil = System.currentTimeMillis() / 1000L - 60L))

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).assertTextContains("View all plans")
        composeRule.onNodeWithTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `test that the CTA buys the promoted plan while the offer is running`() {
        setScreen(
            state = loadedState(validUntil = System.currentTimeMillis() / 1000L + 3600L)
                .copy(hasMultipleOffers = true)
        )

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).assertTextContains("Get Pro I")
        composeRule.onNodeWithTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON).assertExists()
    }

    @Test
    fun `test that buy CTA opens the upgrade screen when the offer has expired`() {
        var bought: Subscription? = null
        var viewedAllPlans = false
        setScreen(
            state = loadedState(validUntil = System.currentTimeMillis() / 1000L - 60L),
            onBuyClick = { bought = it },
            onViewAllPlansClick = { viewedAllPlans = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_BUY_BUTTON).performClick()

        assertThat(viewedAllPlans).isTrue()
        assertThat(bought).isNull()
    }

    @Test
    fun `test that the error state keeps the dismiss affordance available`() {
        var dismissed = false
        setScreen(
            state = SubscriptionOfferState(isLoading = false, isConnected = false),
            onDismiss = { dismissed = true },
        )

        composeRule.onNodeWithTag(TEST_TAG_SUBSCRIPTION_OFFER_SCREEN_DISMISS).performClick()

        assertThat(dismissed).isTrue()
    }
}
