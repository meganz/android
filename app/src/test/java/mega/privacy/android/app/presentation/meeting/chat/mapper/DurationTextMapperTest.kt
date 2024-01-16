package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDurationTextFromSecondsUseCaseTest {

    val underTest = DurationTextMapper()

    @ParameterizedTest(name = "\"{1}\" by {0} seconds")
    @MethodSource("provideTestParameters")
    fun `test that the expected duration text is returned`(
        seconds: Int,
        expectedDuration: String,
    ) {
        assertThat(underTest(seconds)).isEqualTo(expectedDuration)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1000 * 1000, "16:40"),
        Arguments.of(5 * 1000, "00:05"),
        Arguments.of(15 * 1000, "00:15"),
        Arguments.of(3601 * 1000, "01:00:01"),
        Arguments.of(3660 * 1000, "01:01:00"),
    )
}