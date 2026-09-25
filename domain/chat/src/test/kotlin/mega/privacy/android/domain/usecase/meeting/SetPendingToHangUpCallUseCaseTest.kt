package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetPendingToHangUpCallUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: SetPendingToHangUpCallUseCase
    val chatId = 123L

    @BeforeEach
    fun setup() {
        underTest = SetPendingToHangUpCallUseCase(callRepository)
    }

    @Test
    fun `test that set pending hang up call`() = runTest {
        underTest.invoke(chatId = chatId, add = true)
        verify(callRepository).addCallPendingToHangUp(chatId = chatId)
    }

    @Test
    fun `test that remove pending hang up call`() = runTest {
        whenever(callRepository.isPendingToHangUp(chatId)).thenReturn(true)
        underTest.invoke(chatId = chatId, add = false)
        verify(callRepository).removeCallPendingToHangUp(chatId = chatId)
    }
}