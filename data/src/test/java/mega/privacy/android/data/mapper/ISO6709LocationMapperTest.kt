package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ISO6709LocationMapperTest {

    private lateinit var underTest: ISO6709LocationMapper

    @BeforeAll
    fun setUp() {
        underTest = ISO6709LocationMapper()
    }

    @ParameterizedTest(name = "when input is {0}, mapped to {1}")
    @MethodSource("provideParams")
    fun `test that location string can be mapped correctly`(
        locationString: String,
        latitude: Double?,
        longitude: Double?,
    ) {
        val result = underTest(locationString)
        Truth.assertThat(result?.first).isEqualTo(latitude)
        Truth.assertThat(result?.second).isEqualTo(longitude)
    }

    private fun provideParams() = Stream.of(
        Arguments.of("+51.528645-0.073989/", 51.528645, -0.073989),
        Arguments.of(
            "+51.528645;-0.073989;10.0;WGS84;2023-12-13T16:04:00Z",
            51.528645,
            -0.073989
        ),
        Arguments.of(
            "Invalid format",
            null,
            null
        ),
        Arguments.of("", null, null),
    )
}
