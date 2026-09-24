package mega.privacy.android.domain.usecase.billing

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorDismissedSubscriptionOfferCampaignsUseCaseTest {

    private val billingRepository = mock<BillingRepository>()

    private val underTest = MonitorDismissedSubscriptionOfferCampaignsUseCase(billingRepository)

    @BeforeEach
    fun resetMocks() {
        reset(billingRepository)
    }

    @Test
    fun `test that invoke emits the dismissed campaigns from the repository`() = runTest {
        stubDismissedCampaigns(setOf(90210L, 90211L))

        underTest().test {
            assertThat(awaitItem()).containsExactly(90210L, 90211L)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits no campaign when none has been dismissed`() = runTest {
        stubDismissedCampaigns(emptySet())

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke monitors the home banner preference only`() = runTest {
        stubDismissedCampaigns(emptySet())

        underTest()

        verify(billingRepository).monitorDismissedSubscriptionOfferCampaigns()
        verifyNoMoreInteractions(billingRepository)
    }

    private fun stubDismissedCampaigns(campaigns: Set<Long>) {
        whenever(billingRepository.monitorDismissedSubscriptionOfferCampaigns())
            .thenReturn(flowOf(campaigns))
    }
}
