package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DismissSubscriptionOfferCampaignUseCaseTest {

    private val billingRepository = mock<BillingRepository>()

    private val underTest = DismissSubscriptionOfferCampaignUseCase(billingRepository)

    @BeforeEach
    fun resetMocks() {
        reset(billingRepository)
    }

    @Test
    fun `test that invoke dismisses the given campaign`() = runTest {
        underTest(90210L)

        verify(billingRepository).addDismissedSubscriptionOfferCampaign(90210L)
    }

    @Test
    fun `test that invoke dismisses offers belonging to no campaign`() = runTest {
        underTest(Subscription.NO_OFFER_CAMPAIGN_ID)

        verify(billingRepository)
            .addDismissedSubscriptionOfferCampaign(Subscription.NO_OFFER_CAMPAIGN_ID)
    }
}
