package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ABTestFeatureFlagValueMapperTest {
    private lateinit var underTest: ABTestFeatureFlagValueMapper

    @BeforeAll
    fun setup() {
        underTest = ABTestFeatureFlagValueMapper()
    }

    @ParameterizedTest(name = "test that if abtestvalue is {0} the mapper returns {1}")
    @MethodSource("provideTestParameters")
    fun `test that remote feature flag value mapper returns boolean correctly`(
        abTestValueLong: Long,
        expectedBoolean: Boolean,
    ) {
        assertThat(underTest.invoke(abTestValueLong)).isEqualTo(expectedBoolean)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(0L, false),
        Arguments.of(1L, true),
        Arguments.of(11111L, true),
    )
}