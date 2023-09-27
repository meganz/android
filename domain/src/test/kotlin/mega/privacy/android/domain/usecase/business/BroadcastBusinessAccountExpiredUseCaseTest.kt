package mega.privacy.android.domain.usecase.business

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BusinessRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BroadcastBusinessAccountExpiredUseCaseTest {

    private lateinit var underTest: BroadcastBusinessAccountExpiredUseCase

    private val businessRepository = mock<BusinessRepository>()


    @BeforeAll
    fun setUp() {
        underTest = BroadcastBusinessAccountExpiredUseCase(businessRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(businessRepository)
    }

    @Test
    fun `test that invoke invokes broadcast business account expired in the repository`() =
        runTest {
            underTest()
            verify(businessRepository).broadcastBusinessAccountExpired()
        }
}