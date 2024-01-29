package mega.privacy.android.app.presentation.time.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DurationInSecondsTextMapperTest {

    val underTest = DurationInSecondsTextMapper()

    @ParameterizedTest(name = "\"{1}\" by {0} seconds")
    @MethodSource("provideTestParameters")
    fun `test that the expected duration text is returned if seconds`(
        duration: Int,
        expectedDuration: String,
    ) {
        assertThat(underTest(duration.seconds)).isEqualTo(expectedDuration)
    }


    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1000, "16:40"),
        Arguments.of(5, "0:05"),
        Arguments.of(15, "0:15"),
        Arguments.of(3601, "1:00:01"),
        Arguments.of(3660, "1:01:00"),
    )
}