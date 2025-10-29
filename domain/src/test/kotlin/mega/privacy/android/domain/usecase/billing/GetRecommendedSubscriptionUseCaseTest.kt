package mega.privacy.android.domain.usecase.billing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetRecommendedSubscriptionUseCaseTest {
    private lateinit var underTest: GetRecommendedSubscriptionUseCase
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()
    private val getCurrentSubscriptionPlanUseCase = mock<GetCurrentSubscriptionPlanUseCase>()
    private val subscriptionMapper = mock<SubscriptionMapper>()

    @BeforeAll
    fun setUp() {
        underTest = GetRecommendedSubscriptionUseCase(
            getLocalPricingUseCase = getLocalPricingUseCase,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            subscriptionMapper = subscriptionMapper,
        )
    }

    @Test
    fun `test that when current plan is PRO_LITE returns PRO_I subscription`() = runTest {
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(AccountType.PRO_LITE)
        whenever(getAppSubscriptionOptionsUseCase(1)).thenReturn(
            listOf(
                subscriptionOptionLite,
                subscriptionOptionI,
                subscriptionOptionII,
            )
        )

        whenever(getLocalPricingUseCase(Skus.SKU_PRO_I_MONTH)).thenReturn(localPricingProI)
        whenever(subscriptionMapper(subscriptionOptionI, localPricingProI)).thenReturn(subscriptionProI)

        val actual = underTest.invoke()
        assertThat(actual).isEqualTo(subscriptionProI)
    }

    @Test
    fun `test that when current plan is PRO_II returns null as it is the highest plan`() = runTest {
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(AccountType.PRO_II)
        whenever(getAppSubscriptionOptionsUseCase(1)).thenReturn(
            listOf(
                subscriptionOptionLite,
                subscriptionOptionI,
                subscriptionOptionII,
            )
        )

        val actual = underTest.invoke()
        assertThat(actual).isNull()
    }

    @Test
    fun `test that when current plan is not in available plans returns first plan`() = runTest {
        whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(AccountType.PRO_III)
        whenever(getAppSubscriptionOptionsUseCase(1)).thenReturn(
            listOf(
                subscriptionOptionLite,
                subscriptionOptionI,
                subscriptionOptionII,
            )
        )

        whenever(getLocalPricingUseCase(Skus.SKU_PRO_LITE_MONTH)).thenReturn(localPricingLite)
        whenever(subscriptionMapper(subscriptionOptionLite, localPricingLite)).thenReturn(subscriptionLite)

        val actual = underTest.invoke()
        assertThat(actual).isEqualTo(subscriptionLite)
    }

    private val subscriptionOptionLite = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(499L),
        currency = Currency("EUR"),
    )

    private val subscriptionOptionI = SubscriptionOption(
        handle = 1560943707714440503,
        accountType = AccountType.PRO_I,
        months = 1,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyPoint.SystemCurrencyPoint(999L),
        currency = Currency("EUR"),
    )

    private val subscriptionOptionII = SubscriptionOption(
        handle = 7974113413762509455,
        accountType = AccountType.PRO_II,
        months = 1,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyPoint.SystemCurrencyPoint(1999L),
        currency = Currency("EUR"),
    )

    private val localPricingLite = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(499L),
        Currency("EUR"),
        Skus.SKU_PRO_LITE_MONTH,
        emptyList()
    )

    private val localPricingProI = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(999L),
        Currency("EUR"),
        Skus.SKU_PRO_I_MONTH,
        emptyList()
    )

    private val subscriptionLite = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(499L.toFloat(), Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProI = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(999L.toFloat(), Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )
}