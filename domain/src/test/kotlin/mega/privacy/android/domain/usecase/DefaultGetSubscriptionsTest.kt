package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.DefaultGetSubscriptions as DefaultGetSubscriptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_MONTH
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.billing.GetAppSubscriptionOptionsUseCase
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetSubscriptionsTest {
    private lateinit var underTest: GetSubscriptions
    private val accountRepository = mock<AccountRepository>()
    private val getLocalPricing = mock<GetLocalPricing>()
    private val calculateCurrencyAmount = mock<CalculateCurrencyAmount>()
    private val currencyMapper = ::Currency
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()

    private val subscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(9.99.toLong()),
        currency = currencyMapper("EUR"),
    )
    private val subscription = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(9.99.toLong().toFloat(), Currency("EUR"))
    )

    private val localPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(9.99.toLong()),
        Currency("EUR"),
        SKU_PRO_LITE_MONTH
    )

    private val currencyAmount = CurrencyAmount(9.99.toLong().toFloat(), Currency("EUR"))


    @Before
    fun setUp() {
        underTest = DefaultGetSubscriptions(
            accountRepository = accountRepository,
            getLocalPricing = getLocalPricing,
            calculateCurrencyAmount = calculateCurrencyAmount,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
        )
    }

    @Test
    fun `test the GetSubscriptions returns the list of Subscriptions successfully`() {
        runTest {
            whenever(getAppSubscriptionOptionsUseCase()).thenReturn(
                listOf(
                    subscriptionOption
                )
            )
            whenever(getLocalPricing(SKU_PRO_LITE_MONTH)).thenReturn(localPricing)
            whenever(
                calculateCurrencyAmount(
                    CurrencyPoint.LocalCurrencyPoint(9.99.toLong()),
                    Currency("EUR")
                )
            ).thenReturn(currencyAmount)

            val actual = underTest.invoke()

            assertEquals(actual, listOf(subscription))
        }
    }
}