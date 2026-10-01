package mega.privacy.android.domain.usecase.analytics

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.analytics.FirebaseAnalyticsEvent
import mega.privacy.android.domain.repository.FirebaseAnalyticsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendFirebaseAnalyticsEventUseCaseTest {
    private lateinit var underTest: SendFirebaseAnalyticsEventUseCase

    private val firebaseAnalyticsRepository = mock<FirebaseAnalyticsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendFirebaseAnalyticsEventUseCase(
            firebaseAnalyticsRepository = firebaseAnalyticsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(firebaseAnalyticsRepository)
    }

    @Test
    fun `test that invoke passes event to repository`() = runTest {
        underTest(FirebaseAnalyticsEvent.CreateNewAccount)

        verify(firebaseAnalyticsRepository).logEvent(FirebaseAnalyticsEvent.CreateNewAccount)
    }
}
