package test.mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.APP_STORE_URL
import mega.privacy.android.app.utils.PLAY_STORE_URL
import mega.privacy.android.app.utils.isURLSanitizedForWebView
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
        val isMatched = urlToCheck.isURLSanitizedForWebView()
        assertThat(isMatched).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedUrl")
    fun `test that when url does NOT matches regex pattern should NOT accept url`(
        urlToCheck: String,
    ) {
        val isMatched = urlToCheck.isURLSanitizedForWebView()
        assertThat(isMatched).isFalse()
    }

    private fun acceptedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://mega.nz?sort=10?keyword=mega?tracking_id=6423764278462"),
        Arguments.of("https://mega.co.nz?sort=10"),
        Arguments.of("https://mega.co.nz/home"),
        Arguments.of("https://mega.nz/home"),
        Arguments.of("https://mega.io/home"),
        Arguments.of("https://mega.co.nz/#testing"),
        Arguments.of("https://mega.nz/#testing"),
        Arguments.of("https://help.mega.io?sort=DESC"),
        Arguments.of("https://help.mega.io/chat"),
        Arguments.of("https://help.mega.co.nz/chat"),
        Arguments.of("https://help.mega.nz/chat"),
        Arguments.of("https://help.mega.nz/chat?sort=DESC"),
        Arguments.of("https://help.mega.io/chats-meetings/meetings/schedule-oneoff-recurring-meeting"),
        Arguments.of(PLAY_STORE_URL),
        Arguments.of(APP_STORE_URL)
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
        Arguments.of("http://help.mega.io/"),
        Arguments.of("https://mega.co.nz//@attacker.com"),
        Arguments.of("https://mega.co.nz//////@attacker.com"),
        Arguments.of("https://mega.co.nz\\\\@attacker.com"),
        Arguments.of("https://hahdasjdhas.com/"),
        Arguments.of("https://help.mega.nz@attacker.com"),
        Arguments.of("https://attacker.mega.com/"),
        Arguments.of("https://attackermega.nz/"),
        Arguments.of("https://attackermega.nz/home"),
        Arguments.of("https://apps.apple.com/app/mega"),
        Arguments.of("https://play.google.com/store/apps/details?id=mega.privacy.android.app")
    )
}