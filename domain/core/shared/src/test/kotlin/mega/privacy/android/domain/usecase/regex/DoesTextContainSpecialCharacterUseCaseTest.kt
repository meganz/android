package mega.privacy.android.domain.usecase.regex

import com.google.common.truth.Truth.assertThat
import mega.android.authentication.domain.usecase.regex.DoesTextContainSpecialCharacterUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesTextContainSpecialCharacterUseCaseTest {
    private lateinit var underTest: DoesTextContainSpecialCharacterUseCase

    @BeforeAll
    fun setup() {
        underTest = DoesTextContainSpecialCharacterUseCase()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedText")
    fun `test invoke returns true when text contains special character`(
        text: String?,
    ) {
        assertThat(underTest(text)).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedText")
    fun `test that invoke returns false when text does not contain special character`(
        text: String?,
    ) {
        assertThat(underTest(text)).isFalse()
    }

    private fun acceptedText(): Stream<Arguments> = Stream.of(
        Arguments.of("ASDASJAKSasd!@#^&@#^"),
        Arguments.of("AAAAAAA!!"),
        Arguments.of("AHJFHSJKDBSNDNSD&&&&")
    )

    private fun blockedText(): Stream<Arguments> = Stream.of(
        Arguments.of("AAAAAAAAAABBBBBBCCCCC"),
        Arguments.of("aaaaaaaaabbbbbbbccccc"),
        Arguments.of("AJSHAHSJHdasdasjdhasn"),
        Arguments.of("  "),
        Arguments.of(""),
        Arguments.of(null),
    )
}