package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.ChatSessionTermCode
import nz.mega.sdk.MegaChatSession
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionTermCodeMapperTest {
    private val underTest: ChatSessionTermCodeMapper = ChatSessionTermCodeMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(ChatSessionTermCode::class)
    fun `test mapping is not null`(expected: ChatSessionTermCode) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: ChatSessionTermCode, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatSessionTermCode.Recoverable, MegaChatSession.SESS_TERM_CODE_RECOVERABLE),
        Arguments.of(
            ChatSessionTermCode.NonRecoverable,
            MegaChatSession.SESS_TERM_CODE_NON_RECOVERABLE
        ),
        Arguments.of(ChatSessionTermCode.Invalid, -1)
    )
}