package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSubscriptionOfferLastShownTimeUseCaseTest {
    private lateinit var underTest: SetSubscriptionOfferLastShownTimeUseCase

    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()
    private val billingRepository = mock<BillingRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetSubscriptionOfferLastShownTimeUseCase(
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
            billingRepository = billingRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getCurrentTimeInMillisUseCase, billingRepository)
    }

    @Test
    fun `test that invoke saves the current time as the last shown time`() = runTest {
        whenever(getCurrentTimeInMillisUseCase()).thenReturn(NOW)

        underTest()

        verify(billingRepository).setSubscriptionOfferLastShownTime(NOW)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
