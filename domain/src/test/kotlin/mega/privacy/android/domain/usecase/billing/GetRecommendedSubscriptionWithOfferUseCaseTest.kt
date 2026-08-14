package mega.privacy.android.domain.usecase.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.OfferDetail
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
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

        assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
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

        assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
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

            assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
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

        assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
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

    @Test
    fun `test that ignores a plan that has the offer flag but no play billing offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))
        val productsWithoutOffers = listOf(
            megaSku(Skus.SKU_PRO_LITE_MONTH, hasRealOffer = false),
            megaSku(Skus.SKU_PRO_I_MONTH, hasRealOffer = false),
        )
        whenever(billingRepository.querySkus(any())).thenReturn(productsWithoutOffers)

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that returns null when the offer flags do not opt in to being advertised`() =
        runTest {
            val lite = subscriptionOption(
                AccountType.PRO_LITE,
                Skus.SKU_PRO_LITE_MONTH,
                499,
                offer = false,
            )
            val proI = subscriptionOption(
                AccountType.PRO_I,
                Skus.SKU_PRO_I_MONTH,
                999,
                offer = true,
                offerFlags = OTHER_FLAG,
            )
            stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))

            assertThat(underTest.invoke()).isNull()
        }

    @Test
    fun `test that returns null when the offer carries no flags`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(
            AccountType.PRO_I,
            Skus.SKU_PRO_I_MONTH,
            999,
            offer = true,
            offerFlags = null,
        )
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that returns null when the offer flags are empty`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(
            AccountType.PRO_I,
            Skus.SKU_PRO_I_MONTH,
            999,
            offer = true,
            offerFlags = NO_FLAGS,
        )
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))

        assertThat(underTest.invoke()).isNull()
    }

    @Test
    fun `test that returns the plan when the visible bit is set alongside other flags`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(
            AccountType.PRO_I,
            Skus.SKU_PRO_I_MONTH,
            999,
            offer = true,
            offerFlags = VISIBLE_FLAG or OTHER_FLAG,
        )
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))
        val expected = stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
    }

    @Test
    fun `test that skips a hidden offer and returns the next advertised plan`() = runTest {
        val lite = subscriptionOption(
            AccountType.PRO_LITE,
            Skus.SKU_PRO_LITE_MONTH,
            499,
            offer = true,
            offerFlags = OTHER_FLAG,
        )
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        stub(currentPlan = AccountType.FREE, options = listOf(lite, proI))
        val expected = stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        assertThat(underTest.invoke()?.subscription).isEqualTo(expected)
    }

    @Test
    fun `test that does not flag multiple offers when the other discounted plan is hidden`() =
        runTest {
            val lite =
                subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
            val proI =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
            val proII = subscriptionOption(
                AccountType.PRO_II,
                Skus.SKU_PRO_II_MONTH,
                1999,
                offer = true,
                offerFlags = OTHER_FLAG,
            )
            stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI, proII))
            stubMapping(proI, Skus.SKU_PRO_I_MONTH)

            assertThat(underTest.invoke()?.hasMultipleOffers).isFalse()
        }

    @Test
    fun `test that flags multiple offers when two plans have an offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        val proII =
            subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI, proII))
        stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        assertThat(underTest.invoke()?.hasMultipleOffers).isTrue()
    }

    @Test
    fun `test that flags multiple offers when the other discounted plan is not an upgrade`() =
        runTest {
            val lite =
                subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = true)
            val proI =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
            val proII =
                subscriptionOption(AccountType.PRO_II, Skus.SKU_PRO_II_MONTH, 1999, offer = false)
            stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI, proII))
            stubMapping(proI, Skus.SKU_PRO_I_MONTH)

            assertThat(underTest.invoke()?.hasMultipleOffers).isTrue()
        }

    @Test
    fun `test that does not flag multiple offers when only one plan has an offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        stub(currentPlan = AccountType.PRO_LITE, options = listOf(lite, proI))
        stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        assertThat(underTest.invoke()?.hasMultipleOffers).isFalse()
    }

    @Test
    fun `test that does not flag multiple offers when both periods of one plan have an offer`() =
        runTest {
            val proIMonthly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
            val proIYearly =
                subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_YEAR, 9999, offer = true)
            stub(currentPlan = AccountType.FREE, options = listOf(proIMonthly, proIYearly))
            stubMapping(proIMonthly, Skus.SKU_PRO_I_MONTH)

            assertThat(underTest.invoke()?.hasMultipleOffers).isFalse()
        }

    @Test
    fun `test that play billing is not queried when no plan advertises an offer`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = false)
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(AccountType.FREE)
        whenever(getSubscriptionOptionsUseCase()).thenReturn(listOf(lite, proI))

        assertThat(underTest.invoke()).isNull()

        verify(billingRepository, never()).querySkus(any())
    }

    @Test
    fun `test that only the advertised plans are queried against play billing`() = runTest {
        val lite =
            subscriptionOption(AccountType.PRO_LITE, Skus.SKU_PRO_LITE_MONTH, 499, offer = false)
        val proI = subscriptionOption(AccountType.PRO_I, Skus.SKU_PRO_I_MONTH, 999, offer = true)
        stub(currentPlan = AccountType.FREE, options = listOf(lite, proI))
        stubMapping(proI, Skus.SKU_PRO_I_MONTH)

        underTest.invoke()

        verify(billingRepository).querySkus(listOf(Skus.SKU_PRO_I_MONTH))
    }

    private fun subscriptionOption(
        type: AccountType,
        sku: String,
        amount: Long,
        offer: Boolean,
        offerFlags: Long? = VISIBLE_FLAG,
    ) = mock<SubscriptionOption> {
        on { accountType } doReturn type
        on { this.sku } doReturn sku
        on { this.amount } doReturn CurrencyPoint.SystemCurrencyPoint(amount)
        on { hasOffer } doReturn offer
        on { this.offerFlags } doReturn offerFlags
    }

    private suspend fun stub(currentPlan: AccountType, options: List<SubscriptionOption>) {
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(currentPlan)
        whenever(getSubscriptionOptionsUseCase()).thenReturn(options)
        val products = options.map { megaSku(it.sku, hasRealOffer = it.hasOffer) }
        whenever(billingRepository.querySkus(any())).thenReturn(products)
    }

    private fun megaSku(sku: String, hasRealOffer: Boolean) = mock<MegaSku> {
        on { this.sku } doReturn sku
        on { offers } doReturn if (hasRealOffer) listOf(mock<OfferDetail>()) else emptyList()
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

    private companion object {
        private const val VISIBLE_FLAG = 0b0001L
        private const val OTHER_FLAG = 0b0010L
        private const val NO_FLAGS = 0L
    }
}
