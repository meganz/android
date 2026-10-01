package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorWaitingForOtherParticipantsHasEndedUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private lateinit var underTest: MonitorWaitingForOtherParticipantsHasEndedUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorWaitingForOtherParticipantsHasEndedUseCase(callRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        whenever(callRepository.monitorWaitingForOtherParticipantsHasEnded()).thenReturn(flowOf(Pair(123L, true)))
        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(Pair(123L, true))
            cancelAndIgnoreRemainingEvents()
        }
    }
}