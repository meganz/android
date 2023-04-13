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
import mega.privacy.android.app.upgradeAccount.model.UpgradePayment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.usecase.billing.GetCurrentPaymentUseCase
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetSubscriptions
import mega.privacy.android.domain.usecase.billing.IsBillingAvailable
import org.junit.After

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
class UpgradeAccountViewModelTest {
    private lateinit var underTest: UpgradeAccountViewModel

    private val getSubscriptions = mock<GetSubscriptions>()
    private val getCurrentSubscriptionPlan = mock<GetCurrentSubscriptionPlan>()
    private val getCurrentPaymentUseCase = mock<GetCurrentPaymentUseCase>()
    private val isBillingAvailable = mock<IsBillingAvailable>()


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

    private val expectedSubscriptionsList = listOf(
        subscriptionProLiteMonthly,
        subscriptionProIMonthly,
        subscriptionProIIMonthly,
        subscriptionProIIIMonthly
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
            getSubscriptions = getSubscriptions,
            getCurrentSubscriptionPlan = getCurrentSubscriptionPlan,
            getCurrentPaymentUseCase = getCurrentPaymentUseCase,
            isBillingAvailable = isBillingAvailable,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state has all Pro plans listed`() = runTest {
        whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
        underTest.state.map { it.subscriptionsList }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEmpty()
            assertThat(awaitItem()).isSameInstanceAs(expectedSubscriptionsList)
        }
    }

    @Test
    fun `test that current subscribed plan is listed`() =
        runTest {
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
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
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
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
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
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
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
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
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
            whenever(getCurrentPaymentUseCase()).thenReturn(expectedCurrentPayment.currentPayment)

            underTest.setShowBuyNewSubscriptionDialog(expectedShowBuyNewSubscriptionDialog)

            underTest.state.test {
                val showBuyNewSubscriptionDialog = awaitItem().showBuyNewSubscriptionDialog
                assertThat(showBuyNewSubscriptionDialog).isEqualTo(
                    expectedShowBuyNewSubscriptionDialog
                )
            }
        }
}