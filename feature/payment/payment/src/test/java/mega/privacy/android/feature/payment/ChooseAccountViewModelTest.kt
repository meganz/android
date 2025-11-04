package mega.privacy.android.feature.payment

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.entity.payment.Subscriptions
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetSubscriptionsUseCase
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.feature.payment.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.feature.payment.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.feature.payment.presentation.upgrade.ChooseAccountViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChooseAccountViewModelTest {

    private lateinit var underTest: ChooseAccountViewModel

    private val getPricing = mock<GetPricing>()
    private val getSubscriptionsUseCase = mock<GetSubscriptionsUseCase>()
    private val localisedPriceStringMapper = mock<LocalisedPriceStringMapper>()
    private val localisedPriceCurrencyCodeStringMapper =
        mock<LocalisedPriceCurrencyCodeStringMapper>()
    private val formattedSizeMapper = mock<FormattedSizeMapper>()
    private val localisedSubscriptionMapper =
        LocalisedSubscriptionMapper(
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
    private val getRecommendedSubscriptionUseCase =
        mock<GetRecommendedSubscriptionUseCase>()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(
            getSubscriptionsUseCase,
            localisedPriceStringMapper,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
            getRecommendedSubscriptionUseCase,
            getPricing,
            monitorAccountDetailUseCase,
        )
    }

    private fun initViewModel() {
        underTest = ChooseAccountViewModel(
            getPricing = getPricing,
            getSubscriptionsUseCase = getSubscriptionsUseCase,
            localisedSubscriptionMapper = localisedSubscriptionMapper,
            getRecommendedSubscriptionUseCase = getRecommendedSubscriptionUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            savedStateHandle = mock { on { get<Boolean>(any()) }.thenReturn(true) },
        )
    }

    @Test
    fun `test that exception when get pricing is not propagated`() = runTest {
        whenever(getPricing(any())).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            refreshPricing()
            state.map { it.product }.test {
                Truth.assertThat(awaitItem()).isEqualTo(emptyList<Product>())
            }
        }
    }

    @Test
    fun `test that initial state has all Pro plans listed`() = runTest {
        whenever(getPricing(any())).thenReturn(Pricing(emptyList()))
        whenever(getSubscriptionsUseCase()).thenReturn(
            Subscriptions(
                expectedMonthlySubscriptionsList,
                expectedYearlySubscriptionsList
            )
        )
        initViewModel()
        underTest.state.map { it.localisedSubscriptionsList }.test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedLocalisedSubscriptionsList)
        }
    }

    @Test
    fun `test that initial state has cheapest Pro plan`() = runTest {
        val expectedResult = LocalisedSubscription(
            monthlySubscription = subscriptionProLiteMonthly,
            yearlySubscription = subscriptionProLiteMonthly, // ViewModel uses the same subscription for both
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )
        whenever(getPricing(any())).thenReturn(Pricing(emptyList()))
        whenever(getRecommendedSubscriptionUseCase()).thenReturn(subscriptionProLiteMonthly)
        whenever(getSubscriptionsUseCase()).thenReturn(
            Subscriptions(
                expectedMonthlySubscriptionsList,
                expectedYearlySubscriptionsList
            )
        )
        initViewModel()
        underTest.state.map { it.cheapestSubscriptionAvailable }.test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedResult)
        }
    }

    @Test
    fun `test that loadCurrentSubscriptionPlan updates state with current plan and subscription cycle`() =
        runTest {
            val expectedPlan = AccountType.PRO_I
            val expectedCycle = AccountSubscriptionCycle.MONTHLY
            val levelDetail = mock<AccountLevelDetail> {
                on { accountType }.thenReturn(expectedPlan)
                on { accountSubscriptionCycle }.thenReturn(expectedCycle)
            }
            val accountDetail = mock<AccountDetail> {
                on { this.levelDetail }.thenReturn(levelDetail)
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(flowOf(accountDetail))
            whenever(getPricing(any())).thenReturn(Pricing(emptyList()))
            whenever(getSubscriptionsUseCase()).thenReturn(
                Subscriptions(
                    expectedMonthlySubscriptionsList,
                    expectedYearlySubscriptionsList
                )
            )
            initViewModel()

            underTest.state.test {
                val initialState = awaitItem()
                Truth.assertThat(initialState.currentSubscriptionPlan).isEqualTo(expectedPlan)
                Truth.assertThat(initialState.subscriptionCycle).isEqualTo(expectedCycle)
            }
        }

    private val subscriptionProIMonthly = Subscription(
        sku = "mega.android.pro1.onemonth",
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = PRO_I_STORAGE_TRANSFER,
        transfer = PRO_I_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_I_PRICE_MONTHLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIMonthly = Subscription(
        sku = "mega.android.pro2.onemonth",
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = PRO_II_STORAGE_TRANSFER,
        transfer = PRO_II_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_II_PRICE_MONTHLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIIMonthly = Subscription(
        sku = "mega.android.pro3.onemonth",
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = PRO_III_STORAGE_TRANSFER,
        transfer = PRO_III_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_III_PRICE_MONTHLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProLiteMonthly = Subscription(
        sku = "mega.android.prolite.onemonth",
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = PRO_LITE_STORAGE,
        transfer = PRO_LITE_TRANSFER_MONTHLY,
        amount = CurrencyAmount(PRO_LITE_PRICE_MONTHLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val expectedMonthlySubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )

    private val subscriptionProIYearly = Subscription(
        sku = "mega.android.pro1.oneyear",
        accountType = AccountType.PRO_I,
        handle = 7472683699866478542,
        storage = PRO_I_STORAGE_TRANSFER,
        transfer = PRO_I_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_I_PRICE_YEARLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIYearly = Subscription(
        sku = "mega.android.pro2.oneyear",
        accountType = AccountType.PRO_II,
        handle = 370834413380951543,
        storage = PRO_II_STORAGE_TRANSFER,
        transfer = PRO_II_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_II_PRICE_YEARLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProIIIYearly = Subscription(
        sku = "mega.android.pro3.oneyear",
        accountType = AccountType.PRO_III,
        handle = 7225413476571973499,
        storage = PRO_III_STORAGE_TRANSFER,
        transfer = PRO_III_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_III_PRICE_YEARLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val subscriptionProLiteYearly = Subscription(
        sku = "mega.android.prolite.oneyear",
        accountType = AccountType.PRO_LITE,
        handle = -5517769810977460898,
        storage = PRO_LITE_STORAGE,
        transfer = PRO_LITE_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_LITE_PRICE_YEARLY, Currency("EUR")),
        offerId = null,
        discountedAmountMonthly = null,
        discountedPercentage = null,
        offerPeriod = null
    )

    private val expectedYearlySubscriptionsList = listOf(
        subscriptionProLiteYearly,
        subscriptionProIYearly,
        subscriptionProIIYearly,
        subscriptionProIIIYearly
    )

    private val localisedSubscriptionProI = LocalisedSubscription(
        monthlySubscription = subscriptionProIMonthly,
        yearlySubscription = subscriptionProIYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProII = LocalisedSubscription(
        monthlySubscription = subscriptionProIIMonthly,
        yearlySubscription = subscriptionProIIYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProIII = LocalisedSubscription(
        monthlySubscription = subscriptionProIIIMonthly,
        yearlySubscription = subscriptionProIIIYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProLite = LocalisedSubscription(
        monthlySubscription = subscriptionProLiteMonthly,
        yearlySubscription = subscriptionProLiteYearly,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val expectedLocalisedSubscriptionsList = listOf(
        localisedSubscriptionProLite,
        localisedSubscriptionProI,
        localisedSubscriptionProII,
        localisedSubscriptionProIII
    )

    companion object {
        const val PRO_I_STORAGE_TRANSFER = 2048
        const val PRO_II_STORAGE_TRANSFER = 8192
        const val PRO_III_STORAGE_TRANSFER = 16384
        const val PRO_LITE_STORAGE = 400
        const val PRO_LITE_TRANSFER_MONTHLY = 1024
        const val PRO_LITE_TRANSFER_YEARLY = 12288
        const val PRO_I_TRANSFER_YEARLY = 24576
        const val PRO_II_TRANSFER_YEARLY = 98304
        const val PRO_III_TRANSFER_YEARLY = 196608
        const val PRO_I_PRICE_MONTHLY = 9.99F
        const val PRO_II_PRICE_MONTHLY = 19.99F
        const val PRO_III_PRICE_MONTHLY = 29.99F
        const val PRO_LITE_PRICE_MONTHLY = 4.99F
        const val PRO_I_PRICE_YEARLY = 99.99F
        const val PRO_II_PRICE_YEARLY = 199.99F
        const val PRO_III_PRICE_YEARLY = 299.99F
        const val PRO_LITE_PRICE_YEARLY = 49.99F
    }
}