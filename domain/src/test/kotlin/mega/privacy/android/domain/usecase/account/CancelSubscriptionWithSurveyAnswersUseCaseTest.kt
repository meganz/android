package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CancelSubscriptionWithSurveyAnswersUseCaseTest {
    private val billingRepository = mock<BillingRepository>()
    private val underTest = CancelSubscriptionWithSurveyAnswersUseCase(billingRepository)

    @BeforeEach
    fun reset() {
        reset(billingRepository)
    }

    @Test
    fun `test that invoke calls billing repository`() = runTest {
        val reason = "reason"
        val subscriptionId = "subscriptionId"
        val canContact = 1
        underTest(reason, subscriptionId, canContact)
        verify(billingRepository).cancelSubscriptionWithSurveyAnswers(
            reason,
            subscriptionId,
            canContact
        )
    }
}