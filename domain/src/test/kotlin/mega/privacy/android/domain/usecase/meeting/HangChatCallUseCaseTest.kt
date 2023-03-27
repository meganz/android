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

    @Test
    fun `test use-case returns null when call id is -1`() = runTest {
        val actual = underTest(callId = -1L)
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `test use-case returns null when call is hung`() = runTest {
        whenever(repository.hangChatCall(123L)).thenReturn(mockChatRequest)
        whenever(repository.getChatCall(mockChatRequest.chatHandle)).thenReturn(null)
        val actual = underTest(callId = 123L)
        Truth.assertThat(actual).isNull()
    }

    @Test
    fun `test use-case returns null when hangChatCall is failed`() = runTest {
        whenever(repository.hangChatCall(123L)).thenThrow(IllegalStateException("Some exception"))
        val actual = underTest(callId = 123L)
        Truth.assertThat(actual).isNull()
    }
}