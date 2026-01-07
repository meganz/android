package mega.privacy.android.feature.payment

import android.app.Activity
import app.cash.turbine.test
import com.android.billingclient.api.Purchase
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.payment.UpgradeSource
import mega.privacy.android.domain.usecase.billing.MonitorBillingEventUseCase
import mega.privacy.android.domain.usecase.billing.QueryPurchase
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.usecase.GeneratePurchaseUrlUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
internal class BillingViewModelTest {
    private lateinit var underTest: BillingViewModel
    private val queryPurchase = mock<QueryPurchase>()
    private val launchPurchaseFlowUseCase = mock<LaunchPurchaseFlowUseCase>()
    private val generatePurchaseUrlUseCase = mock<GeneratePurchaseUrlUseCase>()
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
            queryPurchase = queryPurchase,
            launchPurchaseFlowUseCase = launchPurchaseFlowUseCase,
            generatePurchaseUrlUseCase = generatePurchaseUrlUseCase,
            monitorBillingEventUseCase = monitorBillingEventUseCase,
        )
    }

    @Test
    fun `test that billingUpdateEvent updated when monitorBillingEvent emit`() = runTest {
        val activeSubscription = MegaPurchase("")
        val event = BillingEvent.OnPurchaseUpdate(
            listOf(
                activeSubscription
            ),
            activeSubscription,
            UpgradeSource.Main
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
    fun `test that launchPurchaseFlowUseCase is called with correct sku for monthly subscription`() =
        runTest {
            val activity = mock<Activity>()
            val expectedSku = Skus.SKU_PRO_I_MONTH
            val source = UpgradeSource.Main
            val subscription = mock<Subscription> {
                on { sku }.thenReturn(expectedSku)
                on { offerId }.thenReturn(null)
            }

            underTest.startPurchase(activity, subscription, source)

            verify(launchPurchaseFlowUseCase).invoke(activity, source, expectedSku, null)
        }

    @Test
    fun `test that launchPurchaseFlowUseCase is called with correct sku for yearly subscription`() =
        runTest {
            val activity = mock<Activity>()
            val expectedSku = Skus.SKU_PRO_II_YEAR
            val source = UpgradeSource.Main
            val subscription = mock<Subscription> {
                on { sku }.thenReturn(expectedSku)
                on { offerId }.thenReturn(null)
            }

            underTest.startPurchase(activity, subscription, source)

            verify(launchPurchaseFlowUseCase).invoke(activity, source, expectedSku, null)
        }

    @Test
    fun `test that launchPurchaseFlowUseCase is called with correct activity and product id`() =
        runTest {
            val activity = mock<Activity>()
            val expectedSku = Skus.SKU_PRO_III_MONTH
            val source = UpgradeSource.Main
            val subscription = mock<Subscription> {
                on { sku }.thenReturn(expectedSku)
                on { offerId }.thenReturn(null)
            }

            underTest.startPurchase(activity, subscription, source)

            verify(launchPurchaseFlowUseCase).invoke(activity, source, expectedSku, null)
        }

    @Test
    fun `test that startPurchase handles PRO_LITE monthly correctly`() = runTest {
        val activity = mock<Activity>()
        val expectedSku = Skus.SKU_PRO_LITE_MONTH
        val source = UpgradeSource.Main
        val subscription = mock<Subscription> {
            on { sku }.thenReturn(expectedSku)
            on { offerId }.thenReturn(null)
        }

        underTest.startPurchase(activity, subscription, source)

        verify(launchPurchaseFlowUseCase).invoke(activity, source, expectedSku, null)
    }

    @Test
    fun `test that startPurchase handles PRO_I yearly correctly with offerId`() = runTest {
        val activity = mock<Activity>()
        val expectedSku = Skus.SKU_PRO_I_YEAR
        val offerId = "offerId"
        val source = UpgradeSource.Main
        val subscription = mock<Subscription> {
            on { sku }.thenReturn(expectedSku)
            on { this.offerId }.thenReturn(offerId)
        }

        underTest.startPurchase(activity, subscription, source)

        verify(launchPurchaseFlowUseCase).invoke(activity, source, expectedSku, offerId)
    }

    @Test
    fun `test that startPurchase does not throw exception when sku is empty`() =
        runTest {
            val activity = mock<Activity>()
            val source = UpgradeSource.Main
            val subscription = mock<Subscription> {
                on { sku }.thenReturn("")
                on { offerId }.thenReturn(null)
            }

            underTest.startPurchase(activity, subscription, source)

            verify(launchPurchaseFlowUseCase).invoke(activity, source, "", null)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick generates URL and updates state for monthly subscription`() =
        runTest {
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"

            whenever(generatePurchaseUrlUseCase(any(), any())).thenReturn(expectedUrl)

            underTest.onExternalPurchaseClick(subscription, monthly = true)
            advanceUntilIdle()

            // Verify the use case was called with correct parameters (monthly = 1)
            verify(generatePurchaseUrlUseCase).invoke("propay_1", 1)

            // Verify the state is updated with the URL
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.onExternalPurchaseClick).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                val triggeredEvent = state.onExternalPurchaseClick as StateEventWithContentTriggered
                assertThat(triggeredEvent.content).isEqualTo(expectedUrl)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick generates URL and updates state for yearly subscription`() =
        runTest {
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_II)
            }
            val expectedUrl =
                "https://mega.nz/#propay_2/uao=Android app Ver 15.21?m=12&session=test"

            whenever(generatePurchaseUrlUseCase(any(), any())).thenReturn(expectedUrl)

            underTest.onExternalPurchaseClick(subscription, monthly = false)
            advanceUntilIdle()

            // Verify the use case was called with correct parameters (yearly = 12)
            verify(generatePurchaseUrlUseCase).invoke("propay_2", 12)

            // Verify the state is updated with the URL
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.onExternalPurchaseClick).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                val triggeredEvent = state.onExternalPurchaseClick as StateEventWithContentTriggered
                assertThat(triggeredEvent.content).isEqualTo(expectedUrl)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick handles different account types correctly`() = runTest {
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_LITE)
        }
        val expectedUrl = "https://mega.nz/#propay_101/uao=Android app Ver 15.21?m=1&session=test"

        whenever(generatePurchaseUrlUseCase(any(), any())).thenReturn(expectedUrl)

        underTest.onExternalPurchaseClick(subscription, monthly = true)
        advanceUntilIdle()

        verify(generatePurchaseUrlUseCase).invoke("propay_101", 1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick handles errors gracefully`() = runTest {
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_I)
        }

        whenever(generatePurchaseUrlUseCase(any(), any())).thenThrow(RuntimeException("Test error"))

        underTest.onExternalPurchaseClick(subscription, monthly = true)
        advanceUntilIdle()

        // Verify error state is set to triggered
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.generalError).isEqualTo(triggered)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that clearExternalPurchaseError resets error state`() = runTest {
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_I)
        }

        whenever(generatePurchaseUrlUseCase(any(), any())).thenThrow(RuntimeException("Test error"))

        underTest.onExternalPurchaseClick(subscription, monthly = true)
        advanceUntilIdle()

        // Verify error state is triggered
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.generalError).isEqualTo(triggered)
        }

        // Clear error
        underTest.clearExternalPurchaseError()

        // Verify error state is consumed (cleared)
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.generalError).isNotEqualTo(triggered)
        }
    }
}
