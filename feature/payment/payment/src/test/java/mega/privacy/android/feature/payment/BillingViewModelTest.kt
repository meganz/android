package mega.privacy.android.feature.payment

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.domain.usecase.billing.QuerySkus
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.model.mapper.AccountTypeToProductIdMapper
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
internal class BillingViewModelTest {
    private lateinit var underTest: BillingViewModel
    private val querySkus = mock<QuerySkus>()
    private val queryPurchase = mock<QueryPurchase>()
    private val launchPurchaseFlowUseCase = mock<LaunchPurchaseFlowUseCase>()
    private val eventFlow = MutableSharedFlow<BillingEvent>()
    private val monitorBillingEventUseCase = mock<MonitorBillingEventUseCase> {
        onBlocking { invoke() }.thenReturn(eventFlow)
    }
    private val accountTypeToProductIdMapper: AccountTypeToProductIdMapper = mock()

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    private fun initViewModel() {
        underTest = BillingViewModel(
            querySkus = querySkus,
            queryPurchase = queryPurchase,
            launchPurchaseFlowUseCase = launchPurchaseFlowUseCase,
            monitorBillingEventUseCase = monitorBillingEventUseCase,
            accountTypeToProductIdMapper = accountTypeToProductIdMapper
        )
    }

    @Test
    fun `test that skus empty when loadSkus return empty`() = runTest {
        whenever(querySkus()).thenReturn(emptyList())
        underTest.loadSkus()
        Truth.assertThat(underTest.skus.value).isEmpty()
    }

    @Test
    fun `test that skus empty when loadSkus throw exception`() = runTest {
        whenever(querySkus()).thenThrow(RuntimeException())
        underTest.loadSkus()
        Truth.assertThat(underTest.skus.value).isEmpty()
    }

    @Test
    fun `test that skus not empty when loadSkus return not empty`() = runTest {
        val list = listOf(MegaSku("", 1L, "USD", emptyList()))
        whenever(querySkus()).thenReturn(list)
        underTest.loadSkus()
        Truth.assertThat(underTest.skus.value).isNotEmpty()
    }

    @Test
    fun `test that purchases empty when loadPurchases return empty`() = runTest {
        whenever(queryPurchase()).thenReturn(emptyList())
        underTest.loadPurchases()
        Truth.assertThat(underTest.purchases.value).isEmpty()
    }

    @Test
    fun `test that purchases empty when loadPurchases throw exception`() = runTest {
        whenever(queryPurchase()).thenThrow(RuntimeException())
        underTest.loadPurchases()
        Truth.assertThat(underTest.purchases.value).isEmpty()
    }

    @Test
    fun `test that purchases not empty when loadPurchases return not empty`() = runTest {
        val list = listOf(MegaPurchase(""))
        whenever(queryPurchase()).thenReturn(list)
        underTest.loadPurchases()
        Truth.assertThat(underTest.purchases.value).isNotEmpty()
    }

    @Test
    fun `test that billingUpdateEvent updated when monitorBillingEvent emit`() = runTest {
        val activeSubscription = MegaPurchase("")
        val event = BillingEvent.OnPurchaseUpdate(
            listOf(
                activeSubscription
            ), activeSubscription
        )
        eventFlow.emit(event)
        Truth.assertThat(underTest.billingUpdateEvent.value).isEqualTo(event)
    }

    @Test
    fun `test that isPurchased return true when state is PURCHASED`() {
        val purchase = MegaPurchase(sku = "", state = Purchase.PurchaseState.PURCHASED)
        Truth.assertThat(underTest.isPurchased(purchase)).isTrue()
    }

    @Test
    fun `test that isPurchased return false when state differ PURCHASED`() {
        val purchase = MegaPurchase(sku = "", state = Purchase.PurchaseState.UNSPECIFIED_STATE)
        Truth.assertThat(underTest.isPurchased(purchase)).isFalse()
    }

    @Test
    fun `test that accountTypeToProductIdMapper is called with correct parameters for monthly subscription`() =
        runTest {
            val activity = mock<Activity>()
            val accountType = AccountType.PRO_I
            val isMonthly = true
            val expectedProductId = Skus.SKU_PRO_I_MONTH

            whenever(accountTypeToProductIdMapper(accountType, isMonthly))
                .thenReturn(expectedProductId)

            underTest.startPurchase(activity, accountType, isMonthly)

            verify(accountTypeToProductIdMapper).invoke(accountType, isMonthly)
        }

    @Test
    fun `test that accountTypeToProductIdMapper is called with correct parameters for yearly subscription`() =
        runTest {
            val activity = mock<Activity>()
            val accountType = AccountType.PRO_II
            val isMonthly = false
            val expectedProductId = Skus.SKU_PRO_II_YEAR

            whenever(accountTypeToProductIdMapper(accountType, isMonthly))
                .thenReturn(expectedProductId)

            underTest.startPurchase(activity, accountType, isMonthly)

            verify(accountTypeToProductIdMapper).invoke(accountType, isMonthly)
        }

    @Test
    fun `test that launchPurchaseFlowUseCase is called with correct activity and product id`() =
        runTest {
            val activity = mock<Activity>()
            val accountType = AccountType.PRO_III
            val isMonthly = true
            val expectedProductId = Skus.SKU_PRO_III_MONTH

            whenever(accountTypeToProductIdMapper(accountType, isMonthly))
                .thenReturn(expectedProductId)

            underTest.startPurchase(activity, accountType, isMonthly)

            verify(launchPurchaseFlowUseCase).invoke(activity, expectedProductId, null)
        }

    @Test
    fun `test that startPurchase handles PRO_LITE monthly correctly`() = runTest {
        val activity = mock<Activity>()
        val accountType = AccountType.PRO_LITE
        val isMonthly = true
        val expectedProductId = Skus.SKU_PRO_LITE_MONTH

        whenever(accountTypeToProductIdMapper(accountType, isMonthly))
            .thenReturn(expectedProductId)

        underTest.startPurchase(activity, accountType, isMonthly)

        verify(accountTypeToProductIdMapper).invoke(accountType, isMonthly)
        verify(launchPurchaseFlowUseCase).invoke(activity, expectedProductId, null)
    }

    @Test
    fun `test that startPurchase handles PRO_I yearly correctly`() = runTest {
        val activity = mock<Activity>()
        val accountType = AccountType.PRO_I
        val isMonthly = false
        val expectedProductId = Skus.SKU_PRO_I_YEAR
        val offerId = "offerId"

        whenever(accountTypeToProductIdMapper(accountType, isMonthly))
            .thenReturn(expectedProductId)

        underTest.startPurchase(activity, accountType, isMonthly, offerId)

        verify(accountTypeToProductIdMapper).invoke(accountType, isMonthly)
        verify(launchPurchaseFlowUseCase).invoke(activity, expectedProductId, offerId)
    }

    @Test
    fun `test that startPurchase does not throw exception when mapper returns empty product id`() =
        runTest {
            val activity = mock<Activity>()
            val accountType = AccountType.FREE
            val isMonthly = true

            whenever(accountTypeToProductIdMapper(accountType, isMonthly))
                .thenReturn("")

            underTest.startPurchase(activity, accountType, isMonthly)

            verify(accountTypeToProductIdMapper).invoke(accountType, isMonthly)
            verify(launchPurchaseFlowUseCase).invoke(activity, "", null)
        }
}