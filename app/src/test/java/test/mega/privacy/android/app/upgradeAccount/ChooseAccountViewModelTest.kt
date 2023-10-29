package test.mega.privacy.android.app.upgradeAccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.upgradeAccount.ChooseAccountViewModel
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetCheapestSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChooseAccountViewModelTest {

    private lateinit var underTest: ChooseAccountViewModel

    private val getPricing = mock<GetPricing>()
    private val getMonthlySubscriptionsUseCase = mock<GetMonthlySubscriptionsUseCase>()
    private val getYearlySubscriptionsUseCase = mock<GetYearlySubscriptionsUseCase>()
    private val localisedPriceStringMapper = mock<LocalisedPriceStringMapper>()
    private val localisedPriceCurrencyCodeStringMapper =
        mock<LocalisedPriceCurrencyCodeStringMapper>()
    private val formattedSizeMapper = mock<FormattedSizeMapper>()
    private val localisedSubscriptionMapper =
        LocalisedSubscriptionMapper(
            localisedPriceStringMapper,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
        )
    private val getCheapestSubscriptionUseCase =
        mock<GetCheapestSubscriptionUseCase>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        reset(
            getMonthlySubscriptionsUseCase,
            getYearlySubscriptionsUseCase,
            localisedPriceStringMapper,
            localisedPriceCurrencyCodeStringMapper,
            formattedSizeMapper,
            getCheapestSubscriptionUseCase,
        )
    }

    private fun initViewModel() {
        underTest = ChooseAccountViewModel(
            getPricing = getPricing,
            getMonthlySubscriptionsUseCase = getMonthlySubscriptionsUseCase,
            getYearlySubscriptionsUseCase = getYearlySubscriptionsUseCase,
            localisedSubscriptionMapper = localisedSubscriptionMapper,
            getCheapestSubscriptionUseCase = getCheapestSubscriptionUseCase,
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that exception when get pricing is not propagated`() = runTest {
        whenever(getPricing(any())).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            refreshPricing()
            state.map { it.product }.test {
                assertEquals(awaitItem(), emptyList())
            }
        }
    }

    @Test
    fun `test that initial state has all Pro plans listed`() = runTest {
        whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
        whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
        initViewModel()
        underTest.state.map { it.localisedSubscriptionsList }.test {
            assertThat(awaitItem()).isEqualTo(expectedLocalisedSubscriptionsList)
        }
    }

    @Test
    fun `test that initial state has cheapest Pro plan`() = runTest {
        whenever(getCheapestSubscriptionUseCase()).thenReturn(subscriptionProLiteMonthly)
        whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
        whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
        initViewModel()
        underTest.state.map { it.cheapestSubscriptionAvailable }.test {
            assertThat(awaitItem()).isEqualTo(subscriptionProLiteMonthly)
        }
    }

    private val subscriptionProIMonthly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = PRO_I_STORAGE_TRANSFER,
        transfer = PRO_I_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_I_PRICE_MONTHLY, Currency("EUR"))
    )

    private val subscriptionProIIMonthly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = PRO_II_STORAGE_TRANSFER,
        transfer = PRO_II_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_II_PRICE_MONTHLY, Currency("EUR"))
    )

    private val subscriptionProIIIMonthly = Subscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = PRO_III_STORAGE_TRANSFER,
        transfer = PRO_III_STORAGE_TRANSFER,
        amount = CurrencyAmount(PRO_III_PRICE_MONTHLY, Currency("EUR"))
    )

    private val subscriptionProLiteMonthly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = PRO_LITE_STORAGE,
        transfer = PRO_LITE_TRANSFER_MONTHLY,
        amount = CurrencyAmount(PRO_LITE_PRICE_MONTHLY, Currency("EUR"))
    )

    private val expectedMonthlySubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
    )

    private val subscriptionProIYearly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 7472683699866478542,
        storage = PRO_I_STORAGE_TRANSFER,
        transfer = PRO_I_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_I_PRICE_YEARLY, Currency("EUR"))
    )

    private val subscriptionProIIYearly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 370834413380951543,
        storage = PRO_II_STORAGE_TRANSFER,
        transfer = PRO_II_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_II_PRICE_YEARLY, Currency("EUR"))
    )

    private val subscriptionProIIIYearly = Subscription(
        accountType = AccountType.PRO_III,
        handle = 7225413476571973499,
        storage = PRO_III_STORAGE_TRANSFER,
        transfer = PRO_III_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_III_PRICE_YEARLY, Currency("EUR"))
    )

    private val subscriptionProLiteYearly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -5517769810977460898,
        storage = PRO_LITE_STORAGE,
        transfer = PRO_LITE_TRANSFER_YEARLY,
        amount = CurrencyAmount(PRO_LITE_PRICE_YEARLY, Currency("EUR"))
    )

    private val expectedYearlySubscriptionsList = listOf(
        subscriptionProLiteYearly,
        subscriptionProIYearly,
        subscriptionProIIYearly,
        subscriptionProIIIYearly
    )

    private val localisedSubscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = PRO_I_STORAGE_TRANSFER,
        monthlyTransfer = PRO_I_STORAGE_TRANSFER,
        yearlyTransfer = PRO_I_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_I_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_I_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = PRO_II_STORAGE_TRANSFER,
        monthlyTransfer = PRO_II_STORAGE_TRANSFER,
        yearlyTransfer = PRO_II_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_II_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_II_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = PRO_III_STORAGE_TRANSFER,
        monthlyTransfer = PRO_III_STORAGE_TRANSFER,
        yearlyTransfer = PRO_III_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_III_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_III_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = PRO_LITE_STORAGE,
        monthlyTransfer = PRO_LITE_TRANSFER_MONTHLY,
        yearlyTransfer = PRO_LITE_TRANSFER_YEARLY,
        monthlyAmount = CurrencyAmount(PRO_LITE_PRICE_MONTHLY, Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            PRO_LITE_PRICE_YEARLY,
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
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