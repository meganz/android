package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDurationTextFromSecondsUseCaseTest {

    val underTest = DurationTextMapper()

    @ParameterizedTest(name = "\"{1}\" by {0} milliseconds")
    @MethodSource("provideMillisecondsTestParameters")
    fun `test that the expected duration text is returned if milliseconds`(
        duration: Int,
        expectedDuration: String,
    ) {
        assertThat(underTest(duration.milliseconds, DurationUnit.MILLISECONDS)).isEqualTo(expectedDuration)
    }

    @ParameterizedTest(name = "\"{1}\" by {0} seconds")
    @MethodSource("provideSecondsTestParameters")
    fun `test that the expected duration text is returned if seconds`(
        duration: Int,
        expectedDuration: String,
    ) {
        assertThat(underTest(duration.seconds, DurationUnit.SECONDS)).isEqualTo(expectedDuration)
    }

    private fun provideMillisecondsTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1000 * 1000, "16:40"),
        Arguments.of(5 * 1000, "0:05"),
        Arguments.of(15 * 1000, "0:15"),
        Arguments.of(3601 * 1000, "1:00:01"),
        Arguments.of(3660 * 1000, "1:01:00"),
    )

    private fun provideSecondsTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1000, "16:40"),
        Arguments.of(5, "0:05"),
        Arguments.of(15, "0:15"),
        Arguments.of(3601, "1:00:01"),
        Arguments.of(3660, "1:01:00"),
    )
}