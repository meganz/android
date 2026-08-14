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
class MonitorDismissedSubscriptionOfferMenuCampaignsUseCaseTest {

    private val billingRepository = mock<BillingRepository>()

    private val underTest = MonitorDismissedSubscriptionOfferMenuCampaignsUseCase(billingRepository)

    @BeforeEach
    fun resetMocks() {
        reset(billingRepository)
    }

    @Test
    fun `test that invoke emits the dismissed menu campaigns from the repository`() = runTest {
        stubDismissedMenuCampaigns(setOf(90210L, 90211L))

        underTest().test {
            assertThat(awaitItem()).containsExactly(90210L, 90211L)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits no campaign when none has been dismissed`() = runTest {
        stubDismissedMenuCampaigns(emptySet())

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke monitors the menu preference only`() = runTest {
        stubDismissedMenuCampaigns(emptySet())

        underTest()

        verify(billingRepository).monitorDismissedSubscriptionOfferMenuCampaigns()
        verifyNoMoreInteractions(billingRepository)
    }

    private fun stubDismissedMenuCampaigns(campaigns: Set<Long>) {
        whenever(billingRepository.monitorDismissedSubscriptionOfferMenuCampaigns())
            .thenReturn(flowOf(campaigns))
    }
}
