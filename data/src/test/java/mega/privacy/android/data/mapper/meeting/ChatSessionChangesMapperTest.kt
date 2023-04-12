package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import nz.mega.sdk.MegaChatSession
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionChangesMapperTest {
    private val underTest: ChatSessionChangesMapper = ChatSessionChangesMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatSessionChanges::class)
    fun `test mapping is not null`(expected: ChatSessionChanges) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatSessionChanges, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatSessionChanges.Status, MegaChatSession.CHANGE_TYPE_STATUS),
        Arguments.of(ChatSessionChanges.RemoteAvFlags, MegaChatSession.CHANGE_TYPE_REMOTE_AVFLAGS),
        Arguments.of(
            ChatSessionChanges.SessionSpeakRequested,
            MegaChatSession.CHANGE_TYPE_SESSION_SPEAK_REQUESTED
        ),
        Arguments.of(
            ChatSessionChanges.SessionOnLowRes,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_LOWRES
        ),
        Arguments.of(
            ChatSessionChanges.SessionOnHiRes,
            MegaChatSession.CHANGE_TYPE_SESSION_ON_HIRES
        ),
        Arguments.of(ChatSessionChanges.SessionOnHold, MegaChatSession.CHANGE_TYPE_SESSION_ON_HOLD),
        Arguments.of(ChatSessionChanges.AudioLevel, MegaChatSession.CHANGE_TYPE_AUDIO_LEVEL),
        Arguments.of(ChatSessionChanges.Permissions, MegaChatSession.CHANGE_TYPE_PERMISSIONS),
        Arguments.of(ChatSessionChanges.NoChanges, -1)
    )
}