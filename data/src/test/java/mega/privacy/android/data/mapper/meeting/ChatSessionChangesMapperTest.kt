package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import nz.mega.sdk.MegaChatSession
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionChangesMapperTest {
    private lateinit var underTest: ChatSessionChangesMapper

    @BeforeAll
    fun setup() {
        underTest = ChatSessionChangesMapper()
    }

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatSessionChanges::class)
    fun `test mapping is not null`(expected: ChatSessionChanges) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @EnumSource(ChatSessionChanges::class)
    fun `test mapped correctly`() {
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_STATUS))
            .contains(ChatSessionChanges.Status)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS))
            .contains(ChatSessionChanges.RemoteAvFlags)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED))
            .contains(ChatSessionChanges.SessionSpeakRequested)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES))
            .contains(ChatSessionChanges.SessionOnLowRes)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES))
            .contains(ChatSessionChanges.SessionOnHiRes)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD))
            .contains(ChatSessionChanges.SessionOnHold)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL))
            .contains(ChatSessionChanges.AudioLevel)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SPEAK_PERMISSION))
            .contains(ChatSessionChanges.SpeakPermissions)
        Truth.assertThat(underTest(MegaChatSession.CHANGE_TYPE_SESSION_ON_RECORDING))
            .contains(ChatSessionChanges.SessionOnRecording)
    }
}