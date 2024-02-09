package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.SpeakerStatusType
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpeakerStatusMapperTest {
    private val underTest: SpeakerStatusMapper = SpeakerStatusMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(SpeakerStatusType::class)
    fun `test mapping is not null`(expected: SpeakerStatusType) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: SpeakerStatusType, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SpeakerStatusType.Disabled, MegaChatCall.SPEAKER_STATUS_DISABLED),
        Arguments.of(SpeakerStatusType.Pending, MegaChatCall.SPEAKER_STATUS_PENDING),
        Arguments.of(SpeakerStatusType.Active, MegaChatCall.SPEAKER_STATUS_ACTIVE),
        Arguments.of(SpeakerStatusType.Unknown, -1),
    )
}