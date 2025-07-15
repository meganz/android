package mega.privacy.android.domain.usecase.billing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.billing.MegaPurchaseState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSuccessfulPurchasesUseCaseTest {

    private lateinit var underTest: MonitorSuccessfulPurchasesUseCase
    private val monitorBillingEventUseCase = mock<MonitorBillingEventUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorSuccessfulPurchasesUseCase(monitorBillingEventUseCase)
    }

    @Test
    fun `test that successful purchase events are emitted when purchase state is Purchased`() =
        runTest {
            val successfulPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
            }
            val billingEvent = BillingEvent.OnPurchaseUpdate(
                purchases = listOf(successfulPurchase),
                activeSubscription = null
            )

            whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent))

            underTest().test {
                val result = awaitItem()
                assertThat(result).isEqualTo(billingEvent)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that purchase events are filtered out when purchase state is not Purchased`() =
        runTest {
            val pendingPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Pending
            }
            val unspecifiedPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Unspecified
            }
            val billingEvent = BillingEvent.OnPurchaseUpdate(
                purchases = listOf(pendingPurchase, unspecifiedPurchase),
                activeSubscription = null
            )

            whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent))

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that purchase events are filtered out when purchases list is empty`() = runTest {
        val billingEvent = BillingEvent.OnPurchaseUpdate(
            purchases = emptyList(),
            activeSubscription = null
        )

        whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent))

        underTest().test {
            awaitComplete()
        }
    }

    @Test
    fun `test that purchase events are filtered out when first purchase state is not Purchased`() =
        runTest {
            val pendingPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Pending
            }
            val successfulPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
            }
            val billingEvent = BillingEvent.OnPurchaseUpdate(
                purchases = listOf(pendingPurchase, successfulPurchase),
                activeSubscription = null
            )

            whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent))

            underTest().test {
                awaitComplete()
            }
        }

    @Test
    fun `test that successful purchase events are emitted when first purchase state is Purchased`() =
        runTest {
            val successfulPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
            }
            val pendingPurchase = mock<MegaPurchase> {
                on { megaPurchaseState } doReturn MegaPurchaseState.Pending
            }
            val billingEvent = BillingEvent.OnPurchaseUpdate(
                purchases = listOf(successfulPurchase, pendingPurchase),
                activeSubscription = null
            )

            whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent))

            underTest().test {
                val result = awaitItem()
                assertThat(result).isEqualTo(billingEvent)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that multiple successful purchase events are emitted in sequence`() = runTest {
        val successfulPurchase1 = mock<MegaPurchase> {
            on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
        }
        val successfulPurchase2 = mock<MegaPurchase> {
            on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
        }
        val billingEvent1 = BillingEvent.OnPurchaseUpdate(
            purchases = listOf(successfulPurchase1),
            activeSubscription = null
        )
        val billingEvent2 = BillingEvent.OnPurchaseUpdate(
            purchases = listOf(successfulPurchase2),
            activeSubscription = null
        )

        whenever(monitorBillingEventUseCase()).thenReturn(flowOf(billingEvent1, billingEvent2))

        underTest().test {
            val result1 = awaitItem()
            val result2 = awaitItem()
            assertThat(result1).isEqualTo(billingEvent1)
            assertThat(result2).isEqualTo(billingEvent2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that mixed purchase events are filtered correctly`() = runTest {
        val successfulPurchase = mock<MegaPurchase> {
            on { megaPurchaseState } doReturn MegaPurchaseState.Purchased
        }
        val pendingPurchase = mock<MegaPurchase> {
            on { megaPurchaseState } doReturn MegaPurchaseState.Pending
        }
        val successfulBillingEvent = BillingEvent.OnPurchaseUpdate(
            purchases = listOf(successfulPurchase),
            activeSubscription = null
        )
        val pendingBillingEvent = BillingEvent.OnPurchaseUpdate(
            purchases = listOf(pendingPurchase),
            activeSubscription = null
        )

        whenever(monitorBillingEventUseCase()).thenReturn(
            flowOf(
                successfulBillingEvent,
                pendingBillingEvent
            )
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(successfulBillingEvent)
            awaitComplete()
        }
    }
}