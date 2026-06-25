package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import nz.mega.sdk.MegaGroupNodesByDateFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaTimelineGranularityIntMapperTest {

    private val underTest = MediaTimelineGranularityIntMapper()

    @ParameterizedTest(name = "when granularity is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the granularity maps to the correct int value`(
        granularity: Granularity,
        expected: Int,
    ) {
        val actual = underTest(granularity)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the granularity is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct granularity`(
        expected: Granularity,
        value: Int,
    ) {
        val actual = underTest(value)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that an unknown int value throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> { underTest(UNKNOWN_VALUE) }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            Granularity.Day,
            MegaGroupNodesByDateFilter.SECTION_GRANULARITY_DAY,
        ),
        Arguments.of(
            Granularity.Month,
            MegaGroupNodesByDateFilter.SECTION_GRANULARITY_MONTH,
        ),
        Arguments.of(
            Granularity.Year,
            MegaGroupNodesByDateFilter.SECTION_GRANULARITY_YEAR,
        ),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
