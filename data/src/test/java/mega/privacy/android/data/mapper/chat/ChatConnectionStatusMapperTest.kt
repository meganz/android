package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatConnectionStatusMapperTest {

    private val underTest = ChatConnectionStatusMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatConnectionStatus::class)
    fun `test mapping is not null`(expected: ChatConnectionStatus) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatConnectionStatus, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatConnectionStatus.Online, MegaChatApi.CHAT_CONNECTION_ONLINE),
        Arguments.of(ChatConnectionStatus.Offline, MegaChatApi.CHAT_CONNECTION_OFFLINE),
        Arguments.of(ChatConnectionStatus.InProgress, MegaChatApi.CHAT_CONNECTION_IN_PROGRESS),
        Arguments.of(ChatConnectionStatus.Logging, MegaChatApi.CHAT_CONNECTION_LOGGING),
        Arguments.of(ChatConnectionStatus.Unknown, -1),
    )

}
