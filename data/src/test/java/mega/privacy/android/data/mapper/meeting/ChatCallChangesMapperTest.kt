package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatCallChangesMapperTest {
    private val underTest: ChatCallChangesMapper = ChatCallChangesMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatCallChanges::class)
    fun `test mapping is not null`(expected: ChatCallChanges) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatCallChanges, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallChanges.Status, MegaChatCall.CHANGE_TYPE_STATUS),
        Arguments.of(ChatCallChanges.LocalAVFlags, MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS),
        Arguments.of(ChatCallChanges.RingingStatus, MegaChatCall.CHANGE_TYPE_RINGING_STATUS),
        Arguments.of(ChatCallChanges.CallComposition, MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION),
        Arguments.of(ChatCallChanges.OnHold, MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD),
        Arguments.of(ChatCallChanges.Speaker, MegaChatCall.CHANGE_TYPE_CALL_SPEAK),
        Arguments.of(ChatCallChanges.AudioLevel, MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL),
        Arguments.of(ChatCallChanges.NetworkQuality, MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY),
        Arguments.of(
            ChatCallChanges.OutgoingRingingStop,
            MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP
        ),
        Arguments.of(ChatCallChanges.WRAllow, MegaChatCall.CHANGE_TYPE_WR_ALLOW),
        Arguments.of(ChatCallChanges.WRDeny, MegaChatCall.CHANGE_TYPE_WR_DENY),
        Arguments.of(ChatCallChanges.WRComposition, MegaChatCall.CHANGE_TYPE_WR_COMPOSITION),
        Arguments.of(ChatCallChanges.WRUsersEntered, MegaChatCall.CHANGE_TYPE_WR_USERS_ENTERED),
        Arguments.of(ChatCallChanges.WRUsersLeave, MegaChatCall.CHANGE_TYPE_WR_USERS_LEAVE),
        Arguments.of(ChatCallChanges.WRUsersAllow, MegaChatCall.CHANGE_TYPE_WR_USERS_ALLOW),
        Arguments.of(ChatCallChanges.WRUsersDeny, MegaChatCall.CHANGE_TYPE_WR_USERS_DENY),
        Arguments.of(
            ChatCallChanges.WRPushedFromCall,
            MegaChatCall.CHANGE_TYPE_WR_PUSHED_FROM_CALL
        ),
        Arguments.of(ChatCallChanges.Unknown, -1)
    )
}