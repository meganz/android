package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.call.ChatCallStatus
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaChatCallStatusMapperTest {
    private val underTest: ChatCallStatusMapper = ChatCallStatusMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatCallStatus::class)
    fun `test mapping is not null`(expected: ChatCallStatus) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatCallStatus, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallStatus.Initial, MegaChatCall.CALL_STATUS_INITIAL),
        Arguments.of(ChatCallStatus.UserNoPresent, MegaChatCall.CALL_STATUS_USER_NO_PRESENT),
        Arguments.of(ChatCallStatus.Connecting, MegaChatCall.CALL_STATUS_CONNECTING),
        Arguments.of(ChatCallStatus.WaitingRoom, MegaChatCall.CALL_STATUS_WAITING_ROOM),
        Arguments.of(ChatCallStatus.Joining, MegaChatCall.CALL_STATUS_JOINING),
        Arguments.of(ChatCallStatus.InProgress, MegaChatCall.CALL_STATUS_IN_PROGRESS),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
        ),
        Arguments.of(ChatCallStatus.Destroyed, MegaChatCall.CALL_STATUS_DESTROYED),
        Arguments.of(ChatCallStatus.Unknown, -1)
    )

    @ParameterizedTest(name = "test change bitmask {0} is mapped to Unknown")
    @MethodSource("provideChangeTypes")
    fun `test that a change bitmask is not mapped to a call status`(changeType: Int) {
        val actual = underTest(changeType)
        Truth.assertThat(actual).isEqualTo(ChatCallStatus.Unknown)
    }

    private fun provideChangeTypes(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaChatCall.CHANGE_TYPE_GENERIC_NOTIFICATION),
        Arguments.of(MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP),
        Arguments.of(MegaChatCall.CHANGE_TYPE_OWN_PERMISSIONS),
        Arguments.of(MegaChatCall.CHANGE_TYPE_WR_ALLOW),
        Arguments.of(MegaChatCall.CHANGE_TYPE_WR_DENY),
        Arguments.of(MegaChatCall.CHANGE_TYPE_WR_COMPOSITION),
        Arguments.of(MegaChatCall.CHANGE_TYPE_CALL_WILL_END),
        Arguments.of(MegaChatCall.CHANGE_TYPE_CALL_LIMITS_UPDATED),
        Arguments.of(MegaChatCall.CHANGE_TYPE_CALL_RAISE_HAND),
    )
}