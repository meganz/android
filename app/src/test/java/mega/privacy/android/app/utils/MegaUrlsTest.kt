package mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.TimberJUnit5Extension
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TimberJUnit5Extension::class)
class MegaUrlsTest {
    @ParameterizedTest(name = "type: {0}")
    @MethodSource("acceptedUrl")
    fun `test that when url does match regex pattern should accept url`(
        urlToCheck: String,
    ) {
        val isMatched = urlToCheck.isURLSanitized()
        assertThat(isMatched).isTrue()
    }

    @ParameterizedTest(name = "type: {0}")
    @MethodSource("blockedUrl")
    fun `test that when url does NOT matches regex pattern should NOT accept url`(
        urlToCheck: String,
    ) {
        val isMatched = urlToCheck.isURLSanitized()
        assertThat(isMatched).isFalse()
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
        Arguments.of("https://help.support.mega.nz/"),
        Arguments.of("https://mega.nz/MEGAsyncSetup64.exe"),
        Arguments.of("https://mega.nz/MEGAsyncSetup32.exe"),
        Arguments.of("https://mega.nz/MEGAsyncSetup.dmg"),
        Arguments.of("https://mega.nz/linux/repo/Arch_Extra/x86_64/megasync-x86_64.pkg.tar.zst"),
        Arguments.of("https://mega.nz/linux/repo/Debian_12/amd64/megasync-Debian_12_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_18.04/amd64/megasync-xUbuntu_18.04_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/Fedora_40/x86_64/megasync-Fedora_40.x86_64.rpm"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megasync-xUbuntu_24.04_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/openSUSE_Leap_15.6/x86_64/megasync-openSUSE_Leap_15.6.x86_64.rpm"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megasync-xUbuntu_24.04_amd64.deb"),
        Arguments.of("https://mega.nz/MEGAvpnSetup64.exe?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.2.1732161101154&__hsfp=1097479502"),
        Arguments.of("https://mega.nz/MEGAcmdSetup64.exe?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.7.1732161101154&__hsfp=1097479502"),
        Arguments.of("https://mega.nz/MEGAcmdSetup32.exe"),
        Arguments.of("https://mega.nz/MEGAcmdSetup.dmg"),
        Arguments.of("https://mega.nz/linux/repo/Arch_Extra/x86_64/megacmd-x86_64.pkg.tar.zst?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.7.1732161101154&__hsfp=1097479502"),
        Arguments.of("https://mega.nz/linux/repo/Debian_12/amd64/megacmd-Debian_12_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_18.04/amd64/megacmd-xUbuntu_18.04_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/Fedora_40/x86_64/megacmd-Fedora_40.x86_64.rpm"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megacmd-xUbuntu_24.04_amd64.deb"),
        Arguments.of("https://mega.nz/linux/repo/openSUSE_Leap_15.5/x86_64/megacmd-openSUSE_Leap_15.5.x86_64.rpm"),
        Arguments.of("https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megacmd-xUbuntu_24.04_amd64.deb"),
        Arguments.of("https://mega.nz/folder/l5ZmhLjJ#vlwwmOIxqZOWLlH_glNQYA"),
        Arguments.of("https://mega.nz/folder/ogBQnbKa#CoRlU5uarTf4XpY3TbPELQ"),
        Arguments.of("https://mega.nz/register/lang_en"),
        Arguments.of("https://mega.nz/register?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.8.1732161101154&__hsfp=1097479502"),
        Arguments.of("https://mega.nz/login/lang_en"),
        Arguments.of("https://mega.nz/propay_1/lang_en"),
        Arguments.of("https://mega.nz/propay_1?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.9.1732161101154&__hsfp=1097479502"),
        Arguments.of("https://mega.nz/propay_2/lang_en"),
        Arguments.of("https://mega.nz/propay_3/lang_en"),
        Arguments.of("https://mega.nz/propay_vpn/lang_en"),
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