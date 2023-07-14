package test.mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Constants.MEGA_REGEXS
import mega.privacy.android.app.utils.Util
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaUrlsTest {
    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedUrl")
    fun `test that when url does matches regex pattern should accept url`(
        urlToCheck: String,
    ) {
        val isMatched = Util.matchRegexs(urlToCheck, MEGA_REGEXS)
        assertThat(isMatched).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedUrl")
    fun `test that when url does NOT matches regex pattern should NOT accept url`(
        urlToCheck: String,
    ) {
        val isMatched = Util.matchRegexs(urlToCheck, MEGA_REGEXS)
        assertThat(isMatched).isFalse()
    }

    private fun acceptedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://mega.nz?sort=10?keyword=mega?tracking_id=6423764278462"),
        Arguments.of("https://mega.co.nz?sort=10"),
        Arguments.of("https://mega.co.nz/home"),
        Arguments.of("https://mega.nz/home"),
        Arguments.of("https://mega.co.nz/#testing"),
        Arguments.of("https://mega.nz/#testing"),
    )

    private fun blockedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://mega.nz@evil.com"),
        Arguments.of("https://mega.co.nz@evil.com"),
        Arguments.of("https://mega.co.nz.attackerurl.com"),
        Arguments.of("https://mega.nz.attackerurl.com"),
        Arguments.of("https://mega.nz@evil.com"),
        Arguments.of("https://mega.co.nzxxx"),
        Arguments.of("https://mega.nzxxx"),
        Arguments.of("https://mega.co.nz"),
        Arguments.of("https://mega.nz"),
        Arguments.of("https://mega.com/"),
        Arguments.of("https://mega.com"),
        Arguments.of("http://mega.nz/"),
        Arguments.of("https://hahdasjdhas.com/"),
    )
}