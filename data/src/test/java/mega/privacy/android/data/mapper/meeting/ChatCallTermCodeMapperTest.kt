package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatCallTermCodeMapperTest {
    private val underTest: ChatCallTermCodeMapper = ChatCallTermCodeMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatCallTermCodeType::class)
    fun `test mapping is not null`(expected: ChatCallTermCodeType) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatCallTermCodeType, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallTermCodeType.Hangup, MegaChatCall.TERM_CODE_HANGUP),
        Arguments.of(
            ChatCallTermCodeType.TooManyParticipants,
            MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS
        ),
        Arguments.of(ChatCallTermCodeType.Reject, MegaChatCall.TERM_CODE_REJECT),
        Arguments.of(ChatCallTermCodeType.Error, MegaChatCall.TERM_CODE_ERROR),
        Arguments.of(ChatCallTermCodeType.NoParticipate, MegaChatCall.TERM_CODE_NO_PARTICIPATE),
        Arguments.of(ChatCallTermCodeType.TooManyClients, MegaChatCall.TERM_CODE_TOO_MANY_CLIENTS),
        Arguments.of(ChatCallTermCodeType.ProtocolVersion, MegaChatCall.TERM_CODE_PROTOCOL_VERSION),
        Arguments.of(ChatCallTermCodeType.Kicked, MegaChatCall.TERM_CODE_KICKED),
        Arguments.of(ChatCallTermCodeType.WaitingRoomTimeout, MegaChatCall.TERM_CODE_WR_TIMEOUT),
        Arguments.of(ChatCallTermCodeType.CallDurationLimit, MegaChatCall.TERM_CODE_CALL_DUR_LIMIT),
        Arguments.of(ChatCallTermCodeType.CallUsersLimit, MegaChatCall.TERM_CODE_CALL_USERS_LIMIT),
        Arguments.of(ChatCallTermCodeType.Invalid, -1)
    )
}