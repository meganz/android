package test.mega.privacy.android.app.upgradeAccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.upgradeAccount.UpgradeAccountViewModel
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.billing.GetMonthlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.GetYearlySubscriptionsUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import org.junit.After

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
class UpgradeAccountViewModelTest {
    private lateinit var underTest: UpgradeAccountViewModel

    private val getMonthlySubscriptionsUseCase = mock<GetMonthlySubscriptionsUseCase>()
    private val getYearlySubscriptionsUseCase = mock<GetYearlySubscriptionsUseCase>()
    private val getCurrentSubscriptionPlanUseCase = mock<GetCurrentSubscriptionPlanUseCase>()
    private val getCurrentPaymentUseCase = mock<GetCurrentPaymentUseCase>()
    private val isBillingAvailableUseCase = mock<IsBillingAvailableUseCase>()
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


    private val subscriptionProIMonthly = Subscription(
        accountType = AccountType.PRO_I,
        handle = 1560943707714440503,
        storage = 2048,
        transfer = 2048,
        amount = CurrencyAmount(9.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIMonthly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 7974113413762509455,
        storage = 8192,
        transfer = 8192,
        amount = CurrencyAmount(19.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIMonthly = Subscription(
        accountType = AccountType.PRO_III,
        handle = -2499193043825823892,
        storage = 16384,
        transfer = 16384,
        amount = CurrencyAmount(29.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteMonthly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -4226692769210777158,
        storage = 400,
        transfer = 1024,
        amount = CurrencyAmount(4.99.toFloat(), Currency("EUR"))
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
        storage = 2048,
        transfer = 24576,
        amount = CurrencyAmount(99.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIYearly = Subscription(
        accountType = AccountType.PRO_II,
        handle = 370834413380951543,
        storage = 8192,
        transfer = 98304,
        amount = CurrencyAmount(199.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProIIIYearly = Subscription(
        accountType = AccountType.PRO_III,
        handle = 7225413476571973499,
        storage = 16384,
        transfer = 196608,
        amount = CurrencyAmount(299.99.toFloat(), Currency("EUR"))
    )

    private val subscriptionProLiteYearly = Subscription(
        accountType = AccountType.PRO_LITE,
        handle = -5517769810977460898,
        storage = 400,
        transfer = 12288,
        amount = CurrencyAmount(49.99.toFloat(), Currency("EUR"))
    )

    private val expectedYearlySubscriptionsList = listOf(
        subscriptionProLiteYearly,
        subscriptionProIYearly,
        subscriptionProIIYearly,
        subscriptionProIIIYearly
    )

    private val localisedSubscriptionProI = LocalisedSubscription(
        accountType = AccountType.PRO_I,
        storage = 2048,
        monthlyTransfer = 2048,
        yearlyTransfer = 24576,
        monthlyAmount = CurrencyAmount(9.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            99.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProII = LocalisedSubscription(
        accountType = AccountType.PRO_II,
        storage = 8192,
        monthlyTransfer = 8192,
        yearlyTransfer = 98304,
        monthlyAmount = CurrencyAmount(19.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            199.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProIII = LocalisedSubscription(
        accountType = AccountType.PRO_III,
        storage = 16384,
        monthlyTransfer = 16384,
        yearlyTransfer = 196608,
        monthlyAmount = CurrencyAmount(29.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            299.99.toFloat(),
            Currency("EUR")
        ),
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )

    private val localisedSubscriptionProLite = LocalisedSubscription(
        accountType = AccountType.PRO_LITE,
        storage = 400,
        monthlyTransfer = 1024,
        yearlyTransfer = 12288,
        monthlyAmount = CurrencyAmount(4.99.toFloat(), Currency("EUR")),
        yearlyAmount = CurrencyAmount(
            49.99.toFloat(),
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

    private val expectedInitialCurrentPlan = AccountType.FREE
    private val expectedCurrentPlan = AccountType.PRO_I
    private val expectedShowBuyNewSubscriptionDialog = true
    private val expectedInitialCurrentPayment = UpgradePayment()
    private val expectedCurrentPayment =
        UpgradePayment(Constants.INVALID_VALUE, PaymentMethod.GOOGLE_WALLET)
    private val expectedCurrentPaymentUpdated =
        UpgradePayment(Constants.PRO_II, PaymentMethod.GOOGLE_WALLET)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = UpgradeAccountViewModel(
            getMonthlySubscriptionsUseCase = getMonthlySubscriptionsUseCase,
            getYearlySubscriptionsUseCase = getYearlySubscriptionsUseCase,
            getCurrentSubscriptionPlanUseCase = getCurrentSubscriptionPlanUseCase,
            getCurrentPaymentUseCase = getCurrentPaymentUseCase,
            isBillingAvailableUseCase = isBillingAvailableUseCase,
            localisedSubscriptionMapper = localisedSubscriptionMapper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state has all Pro plans listed`() = runTest {
        whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
        whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
        underTest.state.map { it.localisedSubscriptionsList }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEmpty()
            assertThat(awaitItem()).isEqualTo(expectedLocalisedSubscriptionsList)
        }
    }

    @Test
    fun `test that current subscribed plan is listed`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            underTest.state.map { it.currentSubscriptionPlan }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedInitialCurrentPlan)
                assertThat(awaitItem()).isEqualTo(expectedCurrentPlan)
            }
        }

    @Test
    fun `test that initial state has current payment listed if current payment is available`() =
        runTest {
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)
            underTest.state.map { it.currentPayment }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedInitialCurrentPayment)
                assertThat(awaitItem()).isEqualTo(expectedCurrentPayment)
            }
        }

    @Test
    fun `test that state is updated when current payment is available and current payment check is called`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.currentPaymentCheck(Constants.PRO_II)

            underTest.state.map { it.currentPayment }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedInitialCurrentPayment)
                assertThat(awaitItem()).isEqualTo(expectedCurrentPayment)
                assertThat(awaitItem()).isEqualTo(expectedCurrentPaymentUpdated)
            }
            underTest.state.map { it.showBuyNewSubscriptionDialog }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedShowBuyNewSubscriptionDialog)
            }
        }

    @Test
    fun `test that showBillingWarning state is set to True`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.setBillingWarningVisibility(true)

            underTest.state.test {
                val showBillingWarning = awaitItem().showBillingWarning
                assertThat(showBillingWarning).isTrue()
            }
        }

    @Test
    fun `test that showBillingWarning state is set to False`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.setBillingWarningVisibility(false)

            underTest.state.test {
                val showBillingWarning = awaitItem().showBillingWarning
                assertThat(showBillingWarning).isFalse()
            }
        }

    @Test
    fun `test that showBuyNewSubscriptionDialog state is updated if setShowBuyNewSubscriptionDialog is called`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.setShowBuyNewSubscriptionDialog(expectedShowBuyNewSubscriptionDialog)

            underTest.state.test {
                val showBuyNewSubscriptionDialog = awaitItem().showBuyNewSubscriptionDialog
                assertThat(showBuyNewSubscriptionDialog).isEqualTo(
                    expectedShowBuyNewSubscriptionDialog
                )
            }
        }

    @Test
    fun `test that isMonthlySelected state is set to True`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.onSelectingMonthlyPlan(true)

            underTest.state.test {
                val isMonthlySelected = awaitItem().isMonthlySelected
                assertThat(isMonthlySelected).isTrue()
            }
        }

    @Test
    fun `test that isMonthlySelected state is set to False`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.onSelectingMonthlyPlan(false)

            underTest.state.test {
                val isMonthlySelected = awaitItem().isMonthlySelected
                assertThat(isMonthlySelected).isFalse()
            }
        }

    @Test
    fun `test that chosenPlan state is set to the plan selected by user`() =
        runTest {
            whenever(getMonthlySubscriptionsUseCase()).thenReturn(expectedMonthlySubscriptionsList)
            whenever(getYearlySubscriptionsUseCase()).thenReturn(expectedYearlySubscriptionsList)
            whenever(getCurrentSubscriptionPlanUseCase()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.onSelectingPlanType(AccountType.PRO_II)

            underTest.state.test {
                val chosenPlan = awaitItem().chosenPlan
                assertThat(chosenPlan).isEqualTo(AccountType.PRO_II)
            }
        }
}