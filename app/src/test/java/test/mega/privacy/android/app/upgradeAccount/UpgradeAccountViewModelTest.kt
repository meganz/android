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
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.usecase.GetCurrentSubscriptionPlan
import mega.privacy.android.domain.usecase.GetSubscriptions
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

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = UpgradeAccountViewModel(
            getSubscriptions = getSubscriptions,
            getCurrentSubscriptionPlan = getCurrentSubscriptionPlan
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
            val expectedInitialCurrentPlan = AccountType.FREE
            val expectedCurrentPlan = AccountType.PRO_I
            whenever(getSubscriptions()).thenReturn(expectedSubscriptionsList)
            whenever(getCurrentSubscriptionPlan()).thenReturn(expectedCurrentPlan)
            underTest.state.map { it.currentSubscriptionPlan }.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(expectedInitialCurrentPlan)
                assertThat(awaitItem()).isEqualTo(expectedCurrentPlan)
            }
        }
}