package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.meeting.SFUDenyType
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SFUDenyMapperTest {
    private val underTest: SFUDenyMapper = SFUDenyMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(SFUDenyType::class)
    fun `test mapping is not null`(expected: SFUDenyType) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: SFUDenyType, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SFUDenyType.Join, MegaChatCall.SFU_DENY_JOIN),
        Arguments.of(SFUDenyType.Audio, MegaChatCall.SFU_DENY_AUDIO),
        Arguments.of(SFUDenyType.Invalid, MegaChatCall.SFU_DENY_INVALID),
        Arguments.of(SFUDenyType.Unknown, 2),
    )
}