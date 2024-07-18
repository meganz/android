package mega.privacy.android.data.mapper.meeting

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.call.CallQualityType
import nz.mega.sdk.MegaChatCall
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallQualityMapperTest {
    private val underTest: CallQualityMapper = CallQualityMapper()

    @ParameterizedTest(name = "test {0} is not mapped to null")
    @EnumSource(CallQualityType::class)
    fun `test mapping is not null`(expected: CallQualityType) {
        val actual = underTest(expected.ordinal)
        Truth.assertThat(actual).isNotNull()
    }

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: CallQualityType, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(CallQualityType.HighDef, MegaChatCall.CALL_QUALITY_HIGH_DEF),
        Arguments.of(CallQualityType.HighMedium, MegaChatCall.CALL_QUALITY_HIGH_MEDIUM),
        Arguments.of(CallQualityType.HighLow, MegaChatCall.CALL_QUALITY_HIGH_LOW),
        Arguments.of(CallQualityType.Unknown, -1),
    )
}