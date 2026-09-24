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
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.exception.LocalPricingNotAvailableException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetSubscriptionsUseCaseTest {
    private lateinit var underTest: GetSubscriptionsUseCase
    private val accountRepository = mock<AccountRepository>()
    private val billingRepository = mock<BillingRepository>()
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()
    private val getSubscriptionOptionsUseCase = mock<GetSubscriptionOptionsUseCase>()
    private val subscriptionMapper = mock<SubscriptionMapper>()

    private val monthlySubscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(9999.toLong()),
        currency = Currency("EUR"),
        sku = "android.test.purchased.month",
        hasOffer = false,
    )

    private val yearlySubscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_LITE,
        months = 12,
        handle = 1560943707714440504,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(99999.toLong()),
        currency = Currency("EUR"),
        sku = SKU_PRO_LITE_YEAR,
        hasOffer = false,
    )

    private val monthlySubscription = Subscription(
        sku = "android.test.purchased.month",
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(9999.toLong().toFloat(), Currency("EUR")),
    )

    private val yearlySubscription = Subscription(
        sku = SKU_PRO_LITE_YEAR,
        accountType = AccountType.PRO_LITE,
        handle = 1560943707714440504,
        storage = 450,
        transfer = 450,
        amount = CurrencyAmount(99999.toLong().toFloat(), Currency("EUR")),
    )

    private val monthlySubscriptionWithInvalidCurrency = monthlySubscription.copy(
        amount = CurrencyAmount(9999.toLong().toFloat(), Currency("INVALID")),
    )

    private val yearlySubscriptionWithInvalidCurrency = yearlySubscription.copy(
        amount = CurrencyAmount(99999.toLong().toFloat(), Currency("")),
    )

    private val monthlyLocalPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(9999.toLong()),
        Currency("EUR"),
        "android.test.purchased.month",
        emptyList()
    )

    private val yearlyLocalPricing = LocalPricing(
        CurrencyPoint.LocalCurrencyPoint(99999.toLong()),
        Currency("EUR"),
        SKU_PRO_LITE_YEAR,
        emptyList()
    )

    @Before
    fun setUp() {
        underTest = GetSubscriptionsUseCase(
            accountRepository = accountRepository,
            billingRepository = billingRepository,
            getLocalPricingUseCase = getLocalPricingUseCase,
            getSubscriptionOptionsUseCase = getSubscriptionOptionsUseCase,
            subscriptionMapper = subscriptionMapper,
        )
    }

    private suspend fun setupCommonMocks() {
        whenever(getSubscriptionOptionsUseCase()).thenReturn(
            listOf(monthlySubscriptionOption, yearlySubscriptionOption)
        )
        whenever(
            billingRepository.querySkus(
                listOf("android.test.purchased.month", SKU_PRO_LITE_YEAR)
            )
        ).thenReturn(emptyList())
        whenever(getLocalPricingUseCase("android.test.purchased.month")).thenReturn(
            monthlyLocalPricing
        )
        whenever(getLocalPricingUseCase(SKU_PRO_LITE_YEAR)).thenReturn(yearlyLocalPricing)
    }

    @Test
    fun `test the GetSubscriptionsUseCase returns the list of Subscriptions successfully`() {
        runTest {
            setupCommonMocks()
            whenever(subscriptionMapper(monthlySubscriptionOption, monthlyLocalPricing)).thenReturn(
                monthlySubscription
            )
            whenever(subscriptionMapper(yearlySubscriptionOption, yearlyLocalPricing)).thenReturn(
                yearlySubscription
            )

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(
                Subscriptions(
                    listOf(monthlySubscription),
                    listOf(yearlySubscription)
                )
            )
        }
    }

    @Test
    fun `test that subscriptions with invalid currency codes are filtered out`() {
        runTest {
            setupCommonMocks()
            whenever(subscriptionMapper(monthlySubscriptionOption, monthlyLocalPricing)).thenReturn(
                monthlySubscriptionWithInvalidCurrency
            )
            whenever(subscriptionMapper(yearlySubscriptionOption, yearlyLocalPricing)).thenReturn(
                yearlySubscriptionWithInvalidCurrency
            )

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(
                Subscriptions(
                    monthlySubscriptions = emptyList(),
                    yearlySubscriptions = emptyList(),
                )
            )
        }
    }

    @Test
    fun `test that only subscriptions with valid currency codes are returned when mixed`() {
        runTest {
            setupCommonMocks()
            whenever(subscriptionMapper(monthlySubscriptionOption, monthlyLocalPricing)).thenReturn(
                monthlySubscription
            )
            whenever(subscriptionMapper(yearlySubscriptionOption, yearlyLocalPricing)).thenReturn(
                yearlySubscriptionWithInvalidCurrency
            )

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(
                Subscriptions(
                    monthlySubscriptions = listOf(monthlySubscription),
                    yearlySubscriptions = emptyList(),
                )
            )
        }
    }

    @Test(expected = LocalPricingNotAvailableException::class)
    fun `test that LocalPricingNotAvailableException is thrown when all local pricing is null`() =
        runTest {
            whenever(getSubscriptionOptionsUseCase()).thenReturn(
                listOf(monthlySubscriptionOption, yearlySubscriptionOption)
            )
            whenever(
                billingRepository.querySkus(
                    listOf("android.test.purchased.month", SKU_PRO_LITE_YEAR)
                )
            ).thenReturn(emptyList())
            whenever(getLocalPricingUseCase("android.test.purchased.month")).thenReturn(null)
            whenever(getLocalPricingUseCase(SKU_PRO_LITE_YEAR)).thenReturn(null)

            underTest.invoke()
        }

    @Test
    fun `test that payment options without local pricing are excluded from results`() =
        runTest {
            whenever(getSubscriptionOptionsUseCase()).thenReturn(
                listOf(monthlySubscriptionOption, yearlySubscriptionOption)
            )
            whenever(
                billingRepository.querySkus(
                    listOf("android.test.purchased.month", SKU_PRO_LITE_YEAR)
                )
            ).thenReturn(emptyList())
            whenever(getLocalPricingUseCase("android.test.purchased.month")).thenReturn(null)
            whenever(getLocalPricingUseCase(SKU_PRO_LITE_YEAR)).thenReturn(yearlyLocalPricing)
            whenever(subscriptionMapper(yearlySubscriptionOption, yearlyLocalPricing)).thenReturn(
                yearlySubscription
            )

            val actual = underTest.invoke()
            assertThat(actual).isEqualTo(
                Subscriptions(
                    monthlySubscriptions = emptyList(),
                    yearlySubscriptions = listOf(yearlySubscription),
                )
            )
        }
}