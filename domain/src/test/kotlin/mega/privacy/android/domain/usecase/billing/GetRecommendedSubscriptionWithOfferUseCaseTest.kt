package mega.privacy.android.domain.usecase.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetRecommendedSubscriptionWithOfferUseCaseTest {
    private lateinit var underTest: GetRecommendedSubscriptionWithOfferUseCase
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val getSubscriptionOptionsUseCase = mock<GetSubscriptionOptionsUseCase>()
    private val getCurrentSubscriptionPlanUseCase = mock<GetCurrentSubscriptionPlanUseCase>()
    private val subscriptionMapper = mock<SubscriptionMapper>()
    private val billingRepository = mock<BillingRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetRecommendedSubscriptionWithOfferUseCase(
            getLocalPricingUseCase = getLocalPricingUseCase,
            getSubscriptionOptionsUseCase = getSubscriptionOptionsUseCase,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            subscriptionMapper = subscriptionMapper,
            billingRepository = billingRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getLocalPricingUseCase,
            getSubscriptionOptionsUseCase,
            getCurrentSubscriptionPlanUseCase,
            subscriptionMapper,
            billingRepository,
        )
    }

    @Test
    fun `test that returns cheapest upgrade tier that has an offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        val proII =
            subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI, proII))
        val expected = stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        assertThat(underTest.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that returns the yearly option when the offer is only on yearly`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proIMonthly =
            subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = false)
        val proIYearly =
            subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_YEAR, 9999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proIMonthly, proIYearly))
        val expected = stubMapping(proIYearly, Skus.SKU_PRO_I_YEAR)

        assertThat(underTest.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that prefers the cheaper period when a tier has an offer on both periods`() =
        runTest {
            val lite = subscriptionOption(
                AccountType.PRO_LITE,
                Skus.SKU_PRO_LITE_MONTH,
                499,
                offer = false
            )
            val proIMonthly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
            val proIYearly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_YEAR, 9999, offer = true)
            stub(
                currentPlan = AccountType.PRO_LITE,
                options = listOf(lite, proIMonthly, proIYearly)
            )
            val expected = stubMapping(proIMonthly, Skus.SKU_PRO_I_MONTH)

            assertThat(underTest.invoke()).isEqualTo(expected)
        }

    @Test
    fun `test that returns null when no upgrade plan has an offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = false)
        val proII =
            subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = false)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI, proII))

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that ignores offers on plans at or below the current plan`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = true)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        val proII =
            subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = false)
        stub(currentPlan = AccountType.PRO_II, options = listOf(lite, proI, proII))

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that ignores plans that are not upgradeable consumer plans`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val business =
            subscriptionOption(AccountType.BUSINESS, "mega.android.business", 9999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, business))

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that returns cheapest offer plan when current plan is free`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = true)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        stub(currentPlan = AccountType.FREE, options = listOf(lite, proI))
        val expected = stubMapping(lite, Skus.SKU_PRO_LITE_MONTH)

        assertThat(underTest.invoke()).isEqualTo(expected)
    }

    @Test
    fun `test that does not recommend the current tier when only its yearly has an offer`() =
        runTest {
            val proIMonthly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = false)
            val proIYearly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_YEAR, 9999, offer = true)
            val proIIMonthly =
                subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = false)
            stub(
                currentPlan = AccountType.PRO_I,
                options = listOf(proIMonthly, proIYearly, proIIMonthly),
            )

            assertThat(underTest.invoke()).isNull()
        }

    private fun subscriptionOption(
        type: AccountType,
        sku: String,
        amount: Long,
        offer: Boolean,
    ) = mock<SubscriptionOption> {
        on { accountType } doReturn type
        on { this.sku } doReturn sku
        on { this.amount } doReturn CurrencyPoint.SystemCurrencyPoint(amount)
        on { hasOffer } doReturn offer
    }

    private suspend fun stub(currentPlan: AccountType, options: List<SubscriptionOption>) {
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(currentPlan)
        whenever(getSubscriptionOptionsUseCase()).thenReturn(options)
    }

    /**
     * Stubs the local-pricing lookup and mapper for [option] so the use case resolves to a fresh
     * [Subscription] mock, and returns that mock for assertion.
     */
    private suspend fun stubMapping(option: SubscriptionOption, sku: String): Subscription {
        val localPricing = mock<LocalPricing>()
        val subscription = mock<Subscription>()
        whenever(getLocalPricingUseCase(sku)).thenReturn(localPricing)
        whenever(subscriptionMapper(option, localPricing)).thenReturn(subscription)
        return subscription
    }
}
