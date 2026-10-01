package mega.privacy.android.domain.usecase.regex

import com.google.common.truth.Truth.assertThat
import mega.android.authentication.domain.usecase.regex.DoesTextContainMixedCaseUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesTextContainMixedCaseUseCaseTest {
    private lateinit var underTest: DoesTextContainMixedCaseUseCase

    @BeforeAll
    fun setup() {
        underTest = DoesTextContainMixedCaseUseCase()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedText")
    fun `test that invoke returns true when text contains both uppercase and lowercase`(
        text: String?,
    ) {
        assertThat(underTest(text)).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedText")
    fun `test that invoke returns false when text does not contain both uppercase and lowercase`(
        text: String?,
    ) {
        assertThat(underTest(text)).isFalse()
    }

    private fun acceptedText(): Stream<Arguments> = Stream.of(
        Arguments.of("ASDASJAKSasd"),
        Arguments.of("AJSKJAKSdfhdsjhfASJAHS"),
        Arguments.of("JASKJASLKAJSKLAJSJa")
    )

    private fun blockedText(): Stream<Arguments> = Stream.of(
        Arguments.of("AAAAAAAAAABBBBBBCCCCC"),
        Arguments.of("aaaaaaaaabbbbbbbccccc"),
        Arguments.of("  "),
        Arguments.of(""),
        Arguments.of(null),
    )
}