package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class IsChatConnectedToInitiateCallUseCaseTest {
    private val chatRepository = mock<ChatRepository>()
    private val underTest = IsChatConnectedToInitiateCallUseCase(chatRepository)
    private val validChatId = 123456L
    private val invalidChatId = -1L
    private val validPeerId = 123456789L
    private val invalidPeerId = -1L
    private val chatRoom = mock<ChatRoom> {
        on { chatId }.thenReturn(validChatId)
    }

    @ParameterizedTest(name = "test when chat connection status is {1} IsChatConnectedToInitiateCallUseCase returns {0}")
    @MethodSource("provideChatConnectionStatus")
    fun `test that for different chat connection status we get expected output`(
        expected: Boolean,
        status: ChatConnectionStatus,
    ) = runTest {
        whenever(chatRepository.getPeerHandle(validChatId, 0)).thenReturn(validPeerId)
        val actual = underTest(status, chatRoom, true, validPeerId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test when chat id is {2} IsChatConnectedToInitiateCallUseCase returns {0}")
    @MethodSource("provideChatRooms")
    fun `test that for different chat room we get expected output`(
        expected: Boolean,
        chatRoom: ChatRoom,
        chatId: Long,
    ) = runTest {
        whenever(chatRepository.getPeerHandle(validChatId, 0)).thenReturn(validPeerId)
        whenever(chatRepository.getPeerHandle(invalidChatId, 0)).thenReturn(invalidPeerId)
        val actual = underTest(ChatConnectionStatus.Online, chatRoom, true, validPeerId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test when peer id is {1} IsChatConnectedToInitiateCallUseCase returns {0}")
    @MethodSource("providePeerIds")
    fun `test that for different peer id we get expected output`(
        expected: Boolean,
        peerId: Long,
    ) = runTest {
        whenever(chatRepository.getPeerHandle(validChatId, 0)).thenReturn(validPeerId)
        whenever(chatRepository.getPeerHandle(invalidChatId, 0)).thenReturn(invalidPeerId)
        val actual = underTest(ChatConnectionStatus.Online, chatRoom, true, peerId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test when peer id is {1} IsChatConnectedToInitiateCallUseCase returns {0}")
    @MethodSource("provideIsWaitingForCall")
    fun `test that for different isWaitingForCall we get expected output`(
        expected: Boolean,
        isWaitingForCall: Boolean,
    ) = runTest {
        whenever(chatRepository.getPeerHandle(validChatId, 0)).thenReturn(validPeerId)
        val actual = underTest(ChatConnectionStatus.Online, chatRoom, isWaitingForCall, validPeerId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideChatConnectionStatus(): Stream<Arguments> = Stream.of(
        Arguments.of(true, ChatConnectionStatus.Online),
        Arguments.of(false, ChatConnectionStatus.Offline),
        Arguments.of(false, ChatConnectionStatus.InProgress),
        Arguments.of(false, ChatConnectionStatus.Logging),
        Arguments.of(false, ChatConnectionStatus.Unknown)
    )

    private fun provideChatRooms(): Stream<Arguments> = Stream.of(
        Arguments.of(true, mock<ChatRoom> { on { chatId }.thenReturn(validChatId) }, validChatId),
        Arguments.of(
            false,
            mock<ChatRoom> { on { chatId }.thenReturn(invalidChatId) },
            invalidChatId
        ),
    )

    private fun providePeerIds(): Stream<Arguments> = Stream.of(
        Arguments.of(true, validPeerId),
        Arguments.of(false, invalidPeerId),
    )

    private fun provideIsWaitingForCall(): Stream<Arguments> = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false)
    )
}