package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BroadcastWaitingForOtherParticipantsHasEndedUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private lateinit var underTest: BroadcastWaitingForOtherParticipantsHasEndedUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = BroadcastWaitingForOtherParticipantsHasEndedUseCase(callRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        underTest(123L, true)
        verify(callRepository).broadcastWaitingForOtherParticipantsHasEnded(123L, true)
    }
}