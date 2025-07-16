package mega.privacy.android.app.appstate.initialisation.postlogin

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.usecase.billing.MonitorSuccessfulPurchasesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PurchaseReviewInitialiserTest {

    private lateinit var underTest: PurchaseReviewInitialiser
    private val monitorSuccessfulPurchasesUseCase = mock<MonitorSuccessfulPurchasesUseCase>()
    private val ratingHandlerImpl = mock<RatingHandlerImpl>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorSuccessfulPurchasesUseCase,
            ratingHandlerImpl
        )
        underTest = PurchaseReviewInitialiser(
            monitorSuccessfulPurchasesUseCase = monitorSuccessfulPurchasesUseCase,
            ratingHandlerImpl = ratingHandlerImpl
        )
    }

    @Test
    fun `test that rating handler is called when successful purchase is detected`() = runTest {

        val billingEvent = mock<BillingEvent.OnPurchaseUpdate>()

        whenever(monitorSuccessfulPurchasesUseCase()).thenReturn(flowOf(billingEvent))

        underTest.invoke("test-session")

        verify(ratingHandlerImpl).updateTransactionFlag(true)
    }

    @Test
    fun `test that rating handler is called multiple times for multiple successful purchases`() =
        runTest {
            val billingEvent1 = mock<BillingEvent.OnPurchaseUpdate>()
            val billingEvent2 = mock<BillingEvent.OnPurchaseUpdate>()

            whenever(monitorSuccessfulPurchasesUseCase()).thenReturn(
                flowOf(
                    billingEvent1,
                    billingEvent2
                )
            )

            underTest.invoke("test-session")

            verify(ratingHandlerImpl, times(2)).updateTransactionFlag(true)
        }

}