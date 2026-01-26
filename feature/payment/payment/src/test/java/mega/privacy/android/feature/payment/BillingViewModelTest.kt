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
import mega.privacy.android.feature.payment.domain.CreateExternalContentLinkTokenUseCase
import mega.privacy.android.domain.entity.billing.ExternalContentLinkResult
import mega.privacy.android.feature.payment.domain.LaunchExternalContentLinkUseCase
import mega.privacy.android.feature.payment.domain.LaunchPurchaseFlowUseCase
import mega.privacy.android.feature.payment.presentation.billing.BillingViewModel
import mega.privacy.android.feature.payment.usecase.GeneratePurchaseUrlUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
internal class BillingViewModelTest {
    private lateinit var underTest: BillingViewModel
    private val queryPurchase = mock<QueryPurchase>()
    private val launchPurchaseFlowUseCase = mock<LaunchPurchaseFlowUseCase>()
    private val generatePurchaseUrlUseCase = mock<GeneratePurchaseUrlUseCase>()
    private val createExternalContentLinkTokenUseCase =
        mock<CreateExternalContentLinkTokenUseCase>()
    private val launchExternalContentLinkUseCase = mock<LaunchExternalContentLinkUseCase>()
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
            createExternalContentLinkTokenUseCase = createExternalContentLinkTokenUseCase,
            launchExternalContentLinkUseCase = launchExternalContentLinkUseCase,
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
            val activity = mock<Activity>()
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"
            val token = "test-token"

            whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
            whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
            whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
                ExternalContentLinkResult.Success
            )

            underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
            advanceUntilIdle()

            // Verify the use case was called with correct parameters (monthly = 1, token)
            verify(generatePurchaseUrlUseCase).invoke("propay_1", 1, token)

            // Verify external content link use case was called
            verify(launchExternalContentLinkUseCase).invoke(any(), anyOrNull())

            // Verify the state is updated with the URL and loading is cleared
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.onExternalPurchaseClick).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                val triggeredEvent = state.onExternalPurchaseClick as StateEventWithContentTriggered
                assertThat(triggeredEvent.content).isEqualTo(expectedUrl)
                assertThat(state.isLoadingExternalCheckout).isFalse()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick generates URL and updates state for yearly subscription`() =
        runTest {
            val activity = mock<Activity>()
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_II)
            }
            val expectedUrl =
                "https://mega.nz/#propay_2/uao=Android app Ver 15.21?m=12&session=test"
            val token = "test-token"

            whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
            whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
            whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
                ExternalContentLinkResult.Success
            )

            underTest.onExternalPurchaseClick(activity, subscription, monthly = false)
            advanceUntilIdle()

            // Verify the use case was called with correct parameters (yearly = 12, token)
            verify(generatePurchaseUrlUseCase).invoke("propay_2", 12, token)

            // Verify external content link use case was called
            verify(launchExternalContentLinkUseCase).invoke(any(), anyOrNull())

            // Verify the state is updated with the URL and loading is cleared
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.onExternalPurchaseClick).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                val triggeredEvent = state.onExternalPurchaseClick as StateEventWithContentTriggered
                assertThat(triggeredEvent.content).isEqualTo(expectedUrl)
                assertThat(state.isLoadingExternalCheckout).isFalse()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick handles different account types correctly`() = runTest {
        val activity = mock<Activity>()
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_LITE)
        }
        val expectedUrl = "https://mega.nz/#propay_101/uao=Android app Ver 15.21?m=1&session=test"
        val token = "test-token"

        whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
        whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
        whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
            ExternalContentLinkResult.Success
        )

        underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
        advanceUntilIdle()

        verify(generatePurchaseUrlUseCase).invoke("propay_101", 1, token)
        verify(launchExternalContentLinkUseCase).invoke(any(), anyOrNull())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick handles errors gracefully`() = runTest {
        val activity = mock<Activity>()
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_I)
        }

        whenever(createExternalContentLinkTokenUseCase()).thenReturn("token")
        whenever(
            generatePurchaseUrlUseCase(
                any(),
                any(),
                any()
            )
        ).thenThrow(RuntimeException("Test error"))

        underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
        advanceUntilIdle()

        // Verify error state is set to triggered and loading is cleared
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.generalError).isEqualTo(triggered)
            assertThat(state.isLoadingExternalCheckout).isFalse()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that clearExternalPurchaseError resets error state`() = runTest {
        val activity = mock<Activity>()
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_I)
        }

        whenever(createExternalContentLinkTokenUseCase()).thenReturn("token")
        whenever(
            generatePurchaseUrlUseCase(
                any(),
                any(),
                any()
            )
        ).thenThrow(RuntimeException("Test error"))

        underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick sets loading state immediately when clicked`() =
        runTest {
            val activity = mock<Activity>()
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"
            val token = "test-token"

            whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
            whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
            whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
                ExternalContentLinkResult.Success
            )

            // Start collecting state updates
            underTest.uiState.test {
                // Skip initial state
                skipItems(1)

                // Start the operation
                underTest.onExternalPurchaseClick(activity, subscription, monthly = true)

                // Verify loading state is set to true immediately
                val loadingState = awaitItem()
                assertThat(loadingState.isLoadingExternalCheckout).isTrue()

                // Wait for completion
                advanceUntilIdle()

                // Verify loading state is cleared after completion
                val finalState = awaitItem()
                assertThat(finalState.isLoadingExternalCheckout).isFalse()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick clears loading state when external content link succeeds`() =
        runTest {
            val activity = mock<Activity>()
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"
            val token = "test-token"

            whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
            whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
            // Use anyOrNull for Uri parameter since toUri() may return null in test environment
            whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
                ExternalContentLinkResult.Success
            )

            underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
            advanceUntilIdle()

            // Verify the use case was called
            verify(launchExternalContentLinkUseCase).invoke(any(), anyOrNull())

            // Verify the state is updated with the URL and loading is cleared
            // StateFlow emits current value immediately, so we should get the final state
            val finalState = underTest.uiState.value
            assertThat(finalState.isLoadingExternalCheckout).isFalse()
            assertThat(finalState.onExternalPurchaseClick).isInstanceOf(
                StateEventWithContentTriggered::class.java
            )
            val triggeredEvent =
                finalState.onExternalPurchaseClick as StateEventWithContentTriggered
            assertThat(triggeredEvent.content).isEqualTo(expectedUrl)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick clears loading state when user cancels`() = runTest {
        val activity = mock<Activity>()
        val subscription = mock<Subscription> {
            on { accountType }.thenReturn(AccountType.PRO_I)
        }
        val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"
        val token = "test-token"

        whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
        whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
        whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
            ExternalContentLinkResult.Cancelled
        )

        underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
        advanceUntilIdle()

        // Verify loading state is cleared after cancellation
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoadingExternalCheckout).isFalse()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onExternalPurchaseClick clears loading state when external content link fails`() =
        runTest {
            val activity = mock<Activity>()
            val subscription = mock<Subscription> {
                on { accountType }.thenReturn(AccountType.PRO_I)
            }
            val expectedUrl = "https://mega.nz/#propay_1/uao=Android app Ver 15.21?m=1&session=test"
            val token = "test-token"

            whenever(createExternalContentLinkTokenUseCase()).thenReturn(token)
            whenever(generatePurchaseUrlUseCase(any(), any(), any())).thenReturn(expectedUrl)
            whenever(launchExternalContentLinkUseCase(any(), anyOrNull())).thenReturn(
                ExternalContentLinkResult.Failed("Test error")
            )

            underTest.onExternalPurchaseClick(activity, subscription, monthly = true)
            advanceUntilIdle()

            // Verify loading state is cleared and error is shown
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoadingExternalCheckout).isFalse()
                assertThat(state.generalError).isEqualTo(triggered)
            }
        }

}
