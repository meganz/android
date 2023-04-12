package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatSessionStatus
import nz.mega.sdk.MegaChatSession
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionStatusMapperTest {
    private val underTest: ChatSessionStatusMapper = ChatSessionStatusMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatSessionStatus::class)
    fun `test mapping is not null`(expected: ChatSessionStatus) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatSessionStatus, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatSessionStatus.Progress, MegaChatSession.SESSION_STATUS_IN_PROGRESS),
        Arguments.of(ChatSessionStatus.Destroyed, MegaChatSession.SESSION_STATUS_DESTROYED),
        Arguments.of(ChatSessionStatus.Invalid, -1)
    )
}