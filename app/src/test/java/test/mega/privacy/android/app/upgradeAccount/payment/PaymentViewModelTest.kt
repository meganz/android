package test.mega.privacy.android.app.upgradeAccount.payment

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.payment.PaymentActivity
import mega.privacy.android.app.upgradeAccount.payment.PaymentViewModel
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetActiveSubscriptionUseCase
import mega.privacy.android.domain.usecase.billing.GetLocalPricingUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import nz.mega.sdk.MegaApiJava
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.reset
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentViewModelTest {
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase = mock()
    private val getPricing: GetPricing = mock()
    private val getLocalPricingUseCase: GetLocalPricingUseCase = mock()
    private val isBillingAvailableUseCase: IsBillingAvailableUseCase = mock()
    private val getActiveSubscriptionUseCase: GetActiveSubscriptionUseCase = mock()
    private val context: Context = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    private lateinit var underTest: PaymentViewModel

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPaymentMethodUseCase,
            getPricing,
            getLocalPricingUseCase,
            isBillingAvailableUseCase,
            getActiveSubscriptionUseCase,
            context,
            savedStateHandle
        )
    }

    private fun initViewModel() {
        underTest = PaymentViewModel(
            getPaymentMethodUseCase = getPaymentMethodUseCase,
            getPricing = getPricing,
            getLocalPricingUseCase = getLocalPricingUseCase,
            isBillingAvailableUseCase = isBillingAvailableUseCase,
            getActiveSubscriptionUseCase = getActiveSubscriptionUseCase,
            context = context,
            savedStateHandle = savedStateHandle
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @ParameterizedTest(name = "test that title and titleColor return correctly when pass upgrade type {0}")
    @MethodSource("provideParametersForTitleAndColor")
    fun `test that title and titleColor return correctly when pass upgrade type`(
        upgradeTypeConstant: Int,
        titleValue: Int,
        titleColorValue: Int,
    ) =
        runTest {
            whenever(getPricing(false)).thenReturn(Pricing(emptyList()))
            whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(
                upgradeTypeConstant
            )
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.title).isEqualTo(titleValue)
                assertThat(state.titleColor).isEqualTo(titleColorValue)
            }
        }

    @ParameterizedTest(name = "test that isPaymentMethodAvailable returns {0} when isBillingAvailableUseCase returns {0} and getPaymentMethodUseCase contains PAYMENT_METHOD_GOOGLE_WALLET")
    @ValueSource(booleans = [true, false])
    fun `test that isPaymentMethodAvailable is set correctly`(boolean: Boolean) =
        runTest {
            whenever(getPricing(false)).thenReturn(Pricing(emptyList()))
            whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
            whenever(isBillingAvailableUseCase()).thenReturn(boolean)
            whenever(getPaymentMethodUseCase(false)).thenReturn(PaymentMethodFlags(1L shl MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET))
            initViewModel()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isPaymentMethodAvailable).isEqualTo(boolean)
            }
        }


    @Test
    fun `test that an exception from getPaymentMethod is not propagated`() = runTest {
        whenever(getPricing(false)).thenReturn(Pricing(emptyList()))
        whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
        whenever(isBillingAvailableUseCase()).thenReturn(true)
        whenever(getPaymentMethodUseCase(false)).thenAnswer {
            throw MegaException(
                1,
                "Not available"
            )
        }
        initViewModel()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isPaymentMethodAvailable).isFalse()
        }
    }

    @Test
    fun `test that an exception from refresh pricing is not propagated`() = runTest {
        whenever(savedStateHandle.get<Int>(PaymentActivity.UPGRADE_TYPE)).thenReturn(PRO_I)
        whenever(isBillingAvailableUseCase()).thenReturn(true)
        whenever(getPricing(false)).thenAnswer { throw MegaException(1, "Not available") }
        initViewModel()
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.monthlyPrice).isEqualTo("")
            assertThat(state.yearlyPrice).isEqualTo("")
        }
    }

    private fun provideParametersForTitleAndColor(): Stream<Arguments> = Stream.of(
        Arguments.of(
            PRO_LITE,
            R.string.prolite_account,
            R.color.orange_400_orange_300
        ),
        Arguments.of(
            PRO_I,
            R.string.pro1_account,
            R.color.red_600_red_300
        ),
        Arguments.of(
            PRO_II,
            R.string.pro2_account,
            R.color.red_600_red_300
        ),
        Arguments.of(
            PRO_III,
            R.string.pro3_account,
            R.color.red_600_red_300
        ),
    )
}