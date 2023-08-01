package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.mega.privacy.android.app.TimberJUnit5Extension
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TimberJUnit5Extension::class)
class IsUrlMatchesRegexUseCaseTest {
    private lateinit var underTest: IsUrlMatchesRegexUseCase

    @BeforeEach
    fun setup() {
        underTest = IsUrlMatchesRegexUseCase()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedUrl")
    fun `test that when url does matches regex pattern should accept url`(
        urlToCheck: String,
    ) {
        assertThat(underTest(urlToCheck, Constants.MEGA_REGEXS)).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedUrl")
    fun `test that when url does NOT matches regex pattern should NOT accept url`(
        urlToCheck: String,
    ) {
        assertThat(IsUrlMatchesRegexUseCase().invoke(urlToCheck, Constants.MEGA_REGEXS)).isFalse()
    }

    private fun acceptedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://mega.nz/"),
        Arguments.of("https://mega.co.nz/"),
        Arguments.of("https://mega.io/"),
        Arguments.of("https://megaad.nz/"),
        Arguments.of("https://mega.nz?sort=10?keyword=mega?tracking_id=6423764278462"),
        Arguments.of("https://mega.co.nz?sort=10"),
        Arguments.of("https://mega.co.nz/home"),
        Arguments.of("https://mega.nz/home"),
        Arguments.of("https://mega.io/home"),
        Arguments.of("https://megaad.nz/home"),
        Arguments.of("https://mega.co.nz/#testing"),
        Arguments.of("https://mega.nz/#testing"),
        Arguments.of("https://support.mega.nz/"),
        Arguments.of("https://support.mega.co.nz/"),
        Arguments.of("https://support.mega.io/"),
        Arguments.of("https://support.megaad.nz/"),
        Arguments.of("https://help.mega.io?sort=DESC"),
        Arguments.of("https://help.mega.io/chat"),
        Arguments.of("https://help.mega.co.nz/chat"),
        Arguments.of("https://help.mega.nz/chat"),
        Arguments.of("https://help.mega.nz/chat?sort=DESC"),
        Arguments.of("https://help.mega.io/chats-meetings/meetings/schedule-oneoff-recurring-meeting"),
        Arguments.of("https://help.megaad.nz/chat"),
        Arguments.of("https://help2.megaad.nz/chat"),
        Arguments.of("https://help.support.mega.nz/")
    )

    private fun blockedUrl(): Stream<Arguments> = Stream.of(
        Arguments.of("https://mega.nz@evil.com"),
        Arguments.of("https://mega.co.nz@evil.com"),
        Arguments.of("https://mega.co.nz.attackerurl.com"),
        Arguments.of("https://mega.nz.attackerurl.com"),
        Arguments.of("https://mega.nz@evil.com"),
        Arguments.of("https://mega.co.nzxxx"),
        Arguments.of("https://mega.nzxxx"),
        Arguments.of("https://mega.com/"),
        Arguments.of("https://mega.com"),
        Arguments.of("http://mega.nz/"),
        Arguments.of("http://mega.nz\n@attacker.com"),
        Arguments.of("http://help.mega.io/"),
        Arguments.of("http://..mega.io/"),
        Arguments.of("https://mega.co.nz//@attacker.com"),
        Arguments.of("https://mega.co.nz//////@attacker.com"),
        Arguments.of("https://mega.co.nz\\\\@attacker.com"),
        Arguments.of("https://hahdasjdhas.com/"),
        Arguments.of("https://help.mega.nz@attacker.com"),
        Arguments.of("https://attacker.mega.com/"),
        Arguments.of("https://attackermega.nz/"),
        Arguments.of("https://attackermega.nz/home"),
        Arguments.of("https://apps.apple.com/app/mega"),
        Arguments.of("https://play.google.com/store/apps/details?id=mega.privacy.android.app"),
    )
}