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
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_MONTH
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetMonthlySubscriptionsUseCaseTest {
    private lateinit var underTest: GetMonthlySubscriptionsUseCase
    private val accountRepository = mock<AccountRepository>()
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val getAppSubscriptionOptionsUseCase = mock<GetAppSubscriptionOptionsUseCase>()
    private val subscriptionMapper = mock<SubscriptionMapper>()

    private val subscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(999.toLong()),
        currency = Currency("EUR"),
    )
    private val subscription = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(999.toLong().toFloat(), Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val localPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(999.toLong()),
        Currency("EUR"),
        SKU_PRO_LITE_MONTH,
        emptyList()
    )

    @Before
    fun setUp() {
        underTest = GetMonthlySubscriptionsUseCase(
            accountRepository = accountRepository,
            getLocalPricingUseCase = getLocalPricingUseCase,
            getAppSubscriptionOptionsUseCase = getAppSubscriptionOptionsUseCase,
            subscriptionMapper = subscriptionMapper,
        )
    }

    @Test
    fun `test the GetSubscriptionsUseCase returns the list of Subscriptions successfully`() {
        runTest {
            whenever(getAppSubscriptionOptionsUseCase(1)).thenReturn(
                listOf(
                    subscriptionOption
                )
            )
            whenever(getLocalPricingUseCase(SKU_PRO_LITE_MONTH)).thenReturn(localPricing)
            whenever(subscriptionMapper(subscriptionOption, localPricing)).thenReturn(subscription)

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(listOf(subscription))
        }
    }
}