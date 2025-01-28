package mega.privacy.android.domain.usecase.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFakeIncomingCallStateUseCaseTest {

    private val callRepository = mock<CallRepository>()
    private val setFakeIncomingCallUseCase = mock<SetFakeIncomingCallStateUseCase>()
    private lateinit var underTest: MonitorFakeIncomingCallStateUseCase
    val map: MutableMap<Long, FakeIncomingCallState> = mutableMapOf()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorFakeIncomingCallStateUseCase(callRepository)
    }

    @Test
    fun `test that it emits value when invoked `() = runTest {
        map[123] = FakeIncomingCallState.Notification
        whenever(callRepository.monitorFakeIncomingCall()).thenReturn(flowOf(map))
        underTest().test {
            setFakeIncomingCallUseCase(chatId = 123, FakeIncomingCallState.Notification)
            Truth.assertThat(awaitItem()).isEqualTo(map)
            cancelAndIgnoreRemainingEvents()
        }
    }
}