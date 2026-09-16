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
    private val underTest: MegaChatCallStatusMapper = MegaChatCallStatusMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatCallStatus::class)
    fun `test mapping is not null`(expected: ChatCallStatus) {
        val actual = underTest(expected)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: Int, raw: ChatCallStatus) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaChatCall.CALL_STATUS_INITIAL, ChatCallStatus.Initial),
        Arguments.of(MegaChatCall.CALL_STATUS_USER_NO_PRESENT, ChatCallStatus.UserNoPresent),
        Arguments.of(MegaChatCall.CALL_STATUS_CONNECTING, ChatCallStatus.Connecting),
        Arguments.of(MegaChatCall.CALL_STATUS_WAITING_ROOM, ChatCallStatus.WaitingRoom),
        Arguments.of(MegaChatCall.CALL_STATUS_JOINING, ChatCallStatus.Joining),
        Arguments.of(MegaChatCall.CALL_STATUS_IN_PROGRESS, ChatCallStatus.InProgress),
        Arguments.of(
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION,
            ChatCallStatus.TerminatingUserParticipation
        ),
        Arguments.of(MegaChatCall.CALL_STATUS_DESTROYED, ChatCallStatus.Destroyed),
        Arguments.of(-1, ChatCallStatus.Unknown)
    )

    @ParameterizedTest(name = "test {0} is not mapped to a change bitmask")
    @EnumSource(ChatCallStatus::class)
    fun `test that a status is never mapped to a change bitmask`(status: ChatCallStatus) {
        val validCallStatuses = listOf(
            MegaChatCall.CALL_STATUS_INITIAL,
            MegaChatCall.CALL_STATUS_USER_NO_PRESENT,
            MegaChatCall.CALL_STATUS_CONNECTING,
            MegaChatCall.CALL_STATUS_WAITING_ROOM,
            MegaChatCall.CALL_STATUS_JOINING,
            MegaChatCall.CALL_STATUS_IN_PROGRESS,
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION,
            MegaChatCall.CALL_STATUS_DESTROYED,
            UNKNOWN_STATUS,
        )

        val actual = underTest(status)

        Truth.assertThat(validCallStatuses).contains(actual)
    }

    private companion object {
        const val UNKNOWN_STATUS = -1
    }
}