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
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_YEAR
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetYearlySubscriptionsUseCaseTest {
    private lateinit var underTest: GetYearlySubscriptionsUseCase
    private val accountRepository = mock<AccountRepository>()
    private val calculateCurrencyAmountUseCase = mock<CalculateCurrencyAmountUseCase>()
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val currencyMapper = ::Currency
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()

    private val subscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(9999.toLong()),
        currency = currencyMapper("EUR"),
    )
    private val subscription = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(9999.toLong().toFloat(), Currency("EUR"))
    )

    private val localPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(9999.toLong()),
        Currency("EUR"),
        SKU_PRO_LITE_YEAR
    )

    private val currencyAmount = CurrencyAmount(9999.toLong().toFloat(), Currency("EUR"))


    @Before
    fun setUp() {
        underTest = GetYearlySubscriptionsUseCase(
            accountRepository = accountRepository,
            calculateCurrencyAmountUseCase = calculateCurrencyAmountUseCase,
            getLocalPricingUseCase = getLocalPricingUseCase,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
        )
    }

    @Test
    fun `test the GetSubscriptionsUseCase returns the list of Subscriptions successfully`() {
        runTest {
            whenever(getAppSubscriptionOptionsUseCase(12)).thenReturn(
                listOf(
                    subscriptionOption
                )
            )
            whenever(getLocalPricingUseCase(SKU_PRO_LITE_YEAR)).thenReturn(localPricing)
            whenever(
                calculateCurrencyAmountUseCase(
                    CurrencyPoint.LocalCurrencyPoint(9999.toLong()),
                    Currency("EUR")
                )
            ).thenReturn(currencyAmount)

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(listOf(subscription))
        }
    }
}