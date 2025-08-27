package mega.privacy.android.feature.payment

import com.android.billingclient.api.Purchase
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.domain.usecase.billing.QuerySkus
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
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
        val list = listOf(MegaSku("", 1L, "USD"))
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
}