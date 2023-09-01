package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatCallChangesMapperTest {

    private lateinit var underTest: ChatCallChangesMapper

    @BeforeAll
    fun setup() {
        underTest = ChatCallChangesMapper()
    }

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatCallChanges::class)
    fun `test mapping is not null`(expected: ChatCallChanges) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @EnumSource(ChatCallChanges::class)
    fun `test mapped correctly`() {
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_STATUS))
            .contains(ChatCallChanges.Status)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS))
            .contains(ChatCallChanges.LocalAVFlags)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_RINGING_STATUS))
            .contains(ChatCallChanges.RingingStatus)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION))
            .contains(ChatCallChanges.CallComposition)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD))
            .contains(ChatCallChanges.OnHold)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_CALL_SPEAK))
            .contains(ChatCallChanges.Speaker)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL))
            .contains(ChatCallChanges.AudioLevel)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY))
            .contains(ChatCallChanges.NetworkQuality)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_OUTGOING_RINGING_STOP))
            .contains(ChatCallChanges.OutgoingRingingStop)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_ALLOW))
            .contains(ChatCallChanges.WaitingRoomAllow)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_DENY))
            .contains(ChatCallChanges.WaitingRoomDeny)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_COMPOSITION))
            .contains(ChatCallChanges.WaitingRoomComposition)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_USERS_ENTERED))
            .contains(ChatCallChanges.WaitingRoomUsersEntered)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_USERS_LEAVE))
            .contains(ChatCallChanges.WaitingRoomUsersLeave)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_USERS_ALLOW))
            .contains(ChatCallChanges.WaitingRoomUsersAllow)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_USERS_DENY))
            .contains(ChatCallChanges.WaitingRoomUsersDeny)
        Truth.assertThat(underTest(MegaChatCall.CHANGE_TYPE_WR_PUSHED_FROM_CALL))
            .contains(ChatCallChanges.WaitingRoomPushedFromCall)
    }

}