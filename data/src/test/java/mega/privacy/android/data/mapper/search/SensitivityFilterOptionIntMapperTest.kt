package mega.privacy.android.data.mapper.search

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_DISABLED
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_FALSE
import nz.mega.sdk.MegaSearchFilter.BOOL_FILTER_ONLY_TRUE
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SensitivityFilterOptionIntMapperTest {

    private val underTest: SensitivityFilterOptionIntMapper = SensitivityFilterOptionIntMapper()

    @ParameterizedTest(name = "when search target is {0}, then the mega search filter value is {1}")
    @MethodSource("provideParameters")
    fun `test that the mapping is correct`(filter: SensitivityFilterOption, expected: Int) {
        val actual = underTest(filter)
        assertThat(expected).isEqualTo(actual)
    }


    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(SensitivityFilterOption.Disabled, BOOL_FILTER_DISABLED),
        Arguments.of(SensitivityFilterOption.SensitiveOnly, BOOL_FILTER_ONLY_FALSE),
        Arguments.of(SensitivityFilterOption.NonSensitiveOnly, BOOL_FILTER_ONLY_TRUE),
    )
}