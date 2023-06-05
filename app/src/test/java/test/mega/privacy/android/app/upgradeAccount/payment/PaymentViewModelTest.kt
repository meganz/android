package test.mega.privacy.android.app.upgradeAccount.payment

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.payment.PaymentActivity
import mega.privacy.android.app.upgradeAccount.payment.PaymentViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetActiveSubscription
import mega.privacy.android.domain.usecase.billing.GetLocalPricingUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import nz.mega.sdk.MegaApiJava
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class PaymentViewModelTest {
    private val getPaymentMethod: GetPaymentMethod = mock()
    private val getPricing: GetPricing = mock()
    private val getLocalPricingUseCase: GetLocalPricingUseCase = mock()
    private val isBillingAvailableUseCase: IsBillingAvailableUseCase = mock()
    private val getActiveSubscription: GetActiveSubscription = mock()
    private val context: Context = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    private lateinit var underTest: PaymentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        underTest = PaymentViewModel(
            getPaymentMethod = getPaymentMethod,
            getPricing = getPricing,
            getLocalPricingUseCase = getLocalPricingUseCase,
            isBillingAvailableUseCase = isBillingAvailableUseCase,
            getActiveSubscription = getActiveSubscription,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that title and titleColor return correctly when pass upgrade type`() = runTest {
        val expectedTitleMap = mapOf(
            PRO_LITE to R.string.prolite_account,
            PRO_I to R.string.pro1_account,
            Constants.PRO_II to R.string.pro2_account,
            Constants.PRO_III to R.string.pro3_account
        )
        val expectedTitleColorMap = mapOf(
            PRO_LITE to R.color.orange_400_orange_300,
            PRO_I to R.color.red_600_red_300,
            Constants.PRO_II to R.color.red_600_red_300,
            Constants.PRO_III to R.color.red_600_red_300
        )
        (PRO_I..PRO_LITE).forEach { upgradeType ->
            whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(upgradeType)
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertEquals(expectedTitleMap[upgradeType], state.title)
                assertEquals(expectedTitleColorMap[upgradeType], state.titleColor)
            }
        }
    }

    @Test
    fun `test that isPaymentMethodAvailable returns true when isBillingAvailableUseCase returns true and getPaymentMethod contains PAYMENT_METHOD_GOOGLE_WALLET`() =
        runTest {
            whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
            whenever(isBillingAvailableUseCase()).thenReturn(true)
            whenever(getPaymentMethod(false)).thenReturn(PaymentMethodFlags(1L shl MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET))
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertTrue(state.isPaymentMethodAvailable)
            }
        }

    @Test
    fun `test that isPaymentMethodAvailable returns false when isBillingAvailableUseCase returns false and getPaymentMethod contains PAYMENT_METHOD_GOOGLE_WALLET`() =
        runTest {
            whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
            whenever(isBillingAvailableUseCase()).thenReturn(false)
            whenever(getPaymentMethod(false)).thenReturn(PaymentMethodFlags(1L shl MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET))
            initViewModel()
            underTest.state.drop(1).test {
                val state = awaitItem()
                assertFalse(state.isPaymentMethodAvailable)
            }
        }


    @Test
    fun `test that an exception from getPaymentMethod is not propagated`() = runTest {
        whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
        whenever(isBillingAvailableUseCase()).thenReturn(true)
        whenever(getPaymentMethod(false)).thenAnswer { throw MegaException(1, "Not available") }
        initViewModel()
        underTest.state.drop(1).test {
            val state = awaitItem()
            assertFalse(state.isPaymentMethodAvailable)
        }
    }

    @Test
    fun `test that an exception from refresh pricing is not propagated`() = runTest {
        whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
        whenever(isBillingAvailableUseCase()).thenReturn(true)
        whenever(getPricing(false)).thenAnswer { throw MegaException(1, "Not available") }
        initViewModel()
        underTest.state.drop(1).test {
            val state = awaitItem()
            assertEquals(state.monthlyPrice, "")
            assertEquals(state.yearlyPrice, "")
        }
    }
}