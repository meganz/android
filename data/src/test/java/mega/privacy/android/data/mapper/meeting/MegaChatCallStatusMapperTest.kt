package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
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
        Arguments.of(ChatCallStatus.Joining, MegaChatCall.CALL_STATUS_JOINING),
        Arguments.of(ChatCallStatus.InProgress, MegaChatCall.CALL_STATUS_IN_PROGRESS),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
        ),
        Arguments.of(ChatCallStatus.Destroyed, MegaChatCall.CALL_STATUS_DESTROYED),
        Arguments.of(ChatCallStatus.Unknown, -1)
    )
}