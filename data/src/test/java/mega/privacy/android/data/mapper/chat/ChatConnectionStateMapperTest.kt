package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatConnectionStateMapperTest {
    private val underTest = ChatConnectionStateMapper(ChatConnectionStatusMapper())

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatConnectionState, handle: Long, status: Int) {
        val actual = underTest(handle, status)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            ChatConnectionState(123456, ChatConnectionStatus.Online),
            123456,
            MegaChatApi.CHAT_CONNECTION_ONLINE
        ),
        Arguments.of(
            ChatConnectionState(123456, ChatConnectionStatus.Offline),
            123456,
            MegaChatApi.CHAT_CONNECTION_OFFLINE
        ),
        Arguments.of(
            ChatConnectionState(123456, ChatConnectionStatus.InProgress),
            123456,
            MegaChatApi.CHAT_CONNECTION_IN_PROGRESS
        ),
        Arguments.of(
            ChatConnectionState(123456, ChatConnectionStatus.Logging),
            123456,
            MegaChatApi.CHAT_CONNECTION_LOGGING
        ),
        Arguments.of(ChatConnectionState(123456, ChatConnectionStatus.Unknown), 123456, -1),
    )
}