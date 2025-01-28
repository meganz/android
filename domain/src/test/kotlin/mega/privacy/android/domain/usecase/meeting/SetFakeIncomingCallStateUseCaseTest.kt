package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetFakeIncomingCallStateUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: SetFakeIncomingCallStateUseCase
    val chatId = 123L

    @BeforeAll
    fun setup() {
        underTest = SetFakeIncomingCallStateUseCase(callRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository)
    }

    @Test
    fun `test that set fake incoming call as Notification`() = runTest {
        underTest.invoke(chatId = chatId, type = FakeIncomingCallState.Notification)
        verify(callRepository).addFakeIncomingCall(
            chatId = chatId,
            type = FakeIncomingCallState.Notification
        )
    }

    @Test
    fun `test that set fake incoming call as a Screen`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
        underTest.invoke(chatId = chatId, type = FakeIncomingCallState.Screen)
        verify(callRepository).addFakeIncomingCall(
            chatId = chatId,
            type = FakeIncomingCallState.Screen
        )
    }

    @Test
    fun `test that set fake incoming call dismissed`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
        underTest.invoke(chatId = chatId, type = FakeIncomingCallState.Dismiss)
        verify(callRepository).addFakeIncomingCall(
            chatId = chatId,
            type = FakeIncomingCallState.Dismiss
        )
    }

    @Test
    fun `test that set fake incoming call removed`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)).thenReturn(FakeIncomingCallState.Notification)
        underTest.invoke(chatId = chatId, type = FakeIncomingCallState.Remove)
        verify(callRepository).addFakeIncomingCall(
            chatId = chatId,
            type = FakeIncomingCallState.Remove
        )
    }
}