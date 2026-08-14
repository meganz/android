package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DismissSubscriptionOfferMenuCampaignUseCaseTest {

    private val billingRepository = mock<BillingRepository>()

    private val underTest = DismissSubscriptionOfferMenuCampaignUseCase(billingRepository)

    @BeforeEach
    fun resetMocks() {
        reset(billingRepository)
    }

    @Test
    fun `test that invoke dismisses the given campaign on the menu only`() = runTest {
        underTest(90210L)

        verify(billingRepository).addDismissedSubscriptionOfferMenuCampaign(90210L)
        verifyNoMoreInteractions(billingRepository)
    }
}
