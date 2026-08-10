package mega.privacy.android.data.mapper.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Sensitivity
import nz.mega.sdk.MegaNodeScopeFilter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaTimelineSensitivityIntMapperTest {

    private val underTest = MediaTimelineSensitivityIntMapper()

    @ParameterizedTest(name = "when sensitivity is {0}, then the int value is {1}")
    @MethodSource("provideParameters")
    fun `test that the sensitivity maps to the correct int value`(
        sensitivity: Sensitivity,
        expected: Int,
    ) {
        val actual = underTest(sensitivity)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "when int value is {1}, then the sensitivity is {0}")
    @MethodSource("provideParameters")
    fun `test that the int value maps back to the correct sensitivity`(
        expected: Sensitivity,
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
            Sensitivity.ShowAll,
            MegaNodeScopeFilter.SENSITIVITY_SHOW_ALL,
        ),
        Arguments.of(
            Sensitivity.HideSensitive,
            MegaNodeScopeFilter.SENSITIVITY_HIDE_SENSITIVE,
        ),
    )

    companion object {
        private const val UNKNOWN_VALUE = -1
    }
}
