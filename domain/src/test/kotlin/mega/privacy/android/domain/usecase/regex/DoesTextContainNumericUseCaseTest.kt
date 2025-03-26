package mega.privacy.android.domain.usecase.regex

import com.google.common.truth.Truth.assertThat
import mega.android.authentication.domain.usecase.regex.DoesTextContainNumericUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesTextContainNumericUseCaseTest {
    private lateinit var underTest: DoesTextContainNumericUseCase

    @BeforeAll
    fun setup() {
        underTest = DoesTextContainNumericUseCase()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedText")
    fun `test that invoke returns true when text contains number`(
        text: String?,
    ) {
        assertThat(underTest(text)).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedText")
    fun `test that invoke returns false when text does not contain number`(
        text: String?,
    ) {
        assertThat(underTest(text)).isFalse()
    }

    private fun acceptedText(): Stream<Arguments> = Stream.of(
        Arguments.of("ajdhasdjkashd123"),
        Arguments.of("1234123u1123"),
        Arguments.of("anasdajdn98081")
    )

    private fun blockedText(): Stream<Arguments> = Stream.of(
        Arguments.of("sdhasjdhaskjdh"),
        Arguments.of("JASJHASJHAJSHA"),
        Arguments.of("  "),
        Arguments.of(""),
        Arguments.of(null),
    )
}