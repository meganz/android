package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDurationTextFromSecondsUseCaseTest {

    val underTest = GetDurationTextFromSecondsUseCase()

    @ParameterizedTest(name = "\"{1}\" by {0} seconds")
    @MethodSource("provideTestParameters")
    fun `test that the expected duration text is returned`(
        seconds: Long,
        expectedDuration: String,
    ) {
        assertThat(underTest(seconds)).isEqualTo(expectedDuration)
    }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(1000L, "16:40"),
        Arguments.of(5L, "00:05"),
        Arguments.of(15L, "00:15"),
        Arguments.of(3601L, "01:00:01"),
        Arguments.of(3660L, "01:01:00"),
    )
}