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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCheapestSubscriptionUseCaseTest {
    private lateinit var underTest: GetCheapestSubscriptionUseCase
    private val calculateCurrencyAmountUseCase = mock<CalculateCurrencyAmountUseCase>()
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val currencyMapper = ::Currency
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetCheapestSubscriptionUseCase(
            calculateCurrencyAmountUseCase = calculateCurrencyAmountUseCase,
            getLocalPricingUseCase = getLocalPricingUseCase,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
        )
    }

    @Test
    fun `test that GetCheapestSubscriptionAvailableUseCase returns cheapest subscription correctly`() {
        runTest {
            whenever(getAppSubscriptionOptionsUseCase(1)).thenReturn(
                listOf(
                    subscriptionOptionI,
                    subscriptionOptionII,
                    subscriptionOptionLite,
                )
            )

            whenever(getLocalPricingUseCase(Skus.SKU_PRO_LITE_MONTH)).thenReturn(localPricing)
            whenever(
                calculateCurrencyAmountUseCase(
                    CurrencyPoint.LocalCurrencyPoint(499L),
                    Currency("EUR")
                )
            ).thenReturn(currencyAmount)

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(subscription)
        }
    }

    private val subscriptionOptionLite = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(499L),
        currency = currencyMapper("EUR"),
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

    private val localPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(499L),
        Currency("EUR"),
        Skus.SKU_PRO_LITE_MONTH
    )

    private val subscription = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(999L.toFloat(), Currency("EUR"))
    )

    private val currencyAmount = CurrencyAmount(999L.toFloat(), Currency("EUR"))
}