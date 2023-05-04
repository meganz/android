package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class IsChatStatusConnectedForCallUseCaseTest {

    private val chatRepository = mock<ChatRepository>()
    private val underTest = IsChatStatusConnectedForCallUseCase(chatRepository)
    private val chatId = Random.nextLong()

    @ParameterizedTest(name = "test when connection status is {0} and chat connection status is {1} use-case returns {2}")
    @MethodSource("provideParams")
    fun `test that use-case returns false if any status is not connected`(
        connectionState: ConnectionState,
        chatConnectionStatus: ChatConnectionStatus,
        expected: Boolean,
    ) = runTest {
        whenever(chatRepository.getConnectionState()).thenReturn(connectionState)
        whenever(chatRepository.getChatConnectionState(chatId = chatId)).thenReturn(
            chatConnectionStatus
        )
        val actual = underTest.invoke(chatId = chatId)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParams(): Stream<Arguments> = Stream.of(
        Arguments.of(ConnectionState.Disconnected, ChatConnectionStatus.Unknown, false),
        Arguments.of(ConnectionState.Disconnected, ChatConnectionStatus.Offline, false),
        Arguments.of(ConnectionState.Disconnected, ChatConnectionStatus.InProgress, false),
        Arguments.of(ConnectionState.Disconnected, ChatConnectionStatus.Logging, false),
        Arguments.of(ConnectionState.Disconnected, ChatConnectionStatus.Online, false),
        Arguments.of(ConnectionState.Connecting, ChatConnectionStatus.Unknown, false),
        Arguments.of(ConnectionState.Connecting, ChatConnectionStatus.Offline, false),
        Arguments.of(ConnectionState.Connecting, ChatConnectionStatus.InProgress, false),
        Arguments.of(ConnectionState.Connecting, ChatConnectionStatus.Logging, false),
        Arguments.of(ConnectionState.Connecting, ChatConnectionStatus.Online, false),
        Arguments.of(ConnectionState.Connected, ChatConnectionStatus.Unknown, false),
        Arguments.of(ConnectionState.Connected, ChatConnectionStatus.Offline, false),
        Arguments.of(ConnectionState.Connected, ChatConnectionStatus.InProgress, false),
        Arguments.of(ConnectionState.Connected, ChatConnectionStatus.Logging, false),
        Arguments.of(ConnectionState.Connected, ChatConnectionStatus.Online, true),
    )
}