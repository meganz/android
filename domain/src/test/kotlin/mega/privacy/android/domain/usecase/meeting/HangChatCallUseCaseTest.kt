package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.repository.CallRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HangChatCallUseCaseTest {
    private val repository = mock<CallRepository>()
    private val underTest = HangChatCallUseCase(repository)
    private val mockChatRequest = mock<ChatRequest> {
        on { type }.thenReturn(ChatRequestType.HangChatCall)
        on { chatHandle }.thenReturn(123456)
        on { userHandle }.thenReturn(123456)
    }

    @Test(expected = IllegalStateException::class)
    fun `test use-case crashes when call id is -1`() = runTest {
        underTest(callId = -1L)
    }

    @Test
    fun `test use-case returns null when call is hung`() = runTest {
        whenever(repository.hangChatCall(123L)).thenReturn(mockChatRequest)
        whenever(repository.getChatCall(mockChatRequest.chatHandle)).thenReturn(null)
        val actual = underTest(callId = 123L)
        Truth.assertThat(actual).isNull()
    }

    @Test(expected = IllegalStateException::class)
    fun `test use-case crashes when hangChatCall is failed`() = runTest {
        whenever(repository.hangChatCall(123L)).thenThrow(IllegalStateException("Some exception"))
        underTest(callId = 123L)
    }
}