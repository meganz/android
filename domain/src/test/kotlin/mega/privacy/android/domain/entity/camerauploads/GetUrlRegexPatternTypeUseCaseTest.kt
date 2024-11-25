package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.IsUrlWhitelistedUseCase
import mega.privacy.android.domain.usecase.IsUrlWhitelistedUseCase.Companion.PLAY_STORE_URL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUrlRegexPatternTypeUseCaseTest {
    private lateinit var underTest: GetUrlRegexPatternTypeUseCase
    private val isUrlMatchesRegexUseCase = IsUrlMatchesRegexUseCase()
    private val isUrlWhitelistedUseCase = IsUrlWhitelistedUseCase()

    @BeforeEach
    fun init() {
        underTest = GetUrlRegexPatternTypeUseCase(
            isUrlMatchesRegexUseCase,
            isUrlWhitelistedUseCase
        )
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("regexType")
    fun `test that when url does matches regex pattern should return correct regex pattern type`(
        name: String,
        urlToCheck: String?,
        pattern: RegexPatternType,
    ) {
        assertThat(underTest(urlToCheck)).isEqualTo(pattern)
    }

    private fun regexType(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "File Link",
            "https://mega.nz/file/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.FILE_LINK
        ),
        Arguments.of(
            "Confirm Link",
            "https://mega.nz/confirm/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.CONFIRMATION_LINK
        ),
        Arguments.of(
            "Folder Link",
            "https://mega.nz/folder/xMpQjZhK#Jp3KceNZRvNsqp",
            RegexPatternType.FOLDER_LINK
        ),
        Arguments.of(
            "Chat Link",
            "https://mega.nz/fm/chat/sffzrasd123",
            RegexPatternType.CHAT_LINK
        ),
        Arguments.of(
            "Purchase Link",
            "https://mega.nz/propay_vpn?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.11.1732161101154&__hsfp=1097479502",
            RegexPatternType.PURCHASE_LINK,
        ),
        Arguments.of(
            "Purchase Link",
            "https://mega.nz/propay_1",
            RegexPatternType.PURCHASE_LINK,
        ),
        Arguments.of(
            "Purchase Link",
            "https://mega.nz/propay_101/lang_en",
            RegexPatternType.PURCHASE_LINK,
        ),
        Arguments.of(
            "Purchase Link",
            "https://mega.nz/propay_101?fajshfalkjshd",
            RegexPatternType.PURCHASE_LINK,
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAsyncSetup64.exe",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAsyncSetup32.exe",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAsyncSetup.dmg",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Arch_Extra/x86_64/megasync-x86_64.pkg.tar.zst",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Debian_12/amd64/megasync-Debian_12_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_18.04/amd64/megasync-xUbuntu_18.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Fedora_40/x86_64/megasync-Fedora_40.x86_64.rpm",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megasync-xUbuntu_24.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/openSUSE_Leap_15.6/x86_64/megasync-openSUSE_Leap_15.6.x86_64.rpm",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megasync-xUbuntu_24.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAvpnSetup64.exe?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.2.1732161101154&__hsfp=1097479502",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAcmdSetup64.exe?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.7.1732161101154&__hsfp=1097479502",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAcmdSetup32.exe",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/MEGAcmdSetup.dmg",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Arch_Extra/x86_64/megacmd-x86_64.pkg.tar.zst?__hstc=254534871.95fd90faf0ef4d78f279814b949b71a9.1731921159722.1731921159722.1732161101154.2&__hssc=254534871.7.1732161101154&__hsfp=1097479502",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Debian_12/amd64/megacmd-Debian_12_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_18.04/amd64/megacmd-xUbuntu_18.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/Fedora_40/x86_64/megacmd-Fedora_40.x86_64.rpm",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megacmd-xUbuntu_24.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/openSUSE_Leap_15.5/x86_64/megacmd-openSUSE_Leap_15.5.x86_64.rpm",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megacmd-xUbuntu_24.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of("Play Store", PLAY_STORE_URL, RegexPatternType.WHITELISTED_URL),
        Arguments.of("Upgrade Page", "https://mega.nz/pro", RegexPatternType.UPGRADE_PAGE_LINK),
        Arguments.of("Upgrade Page", "https://mega.co.nz/pro", RegexPatternType.UPGRADE_PAGE_LINK),
        Arguments.of(
            "Open Device Center Link",
            "https://mega.nz/devicecenter",
            RegexPatternType.OPEN_DEVICE_CENTER_LINK
        ),
        Arguments.of(
            "Open Sync Folder Link",
            "https://mega.nz/opensync#1234567890",
            RegexPatternType.OPEN_SYNC_MEGA_FOLDER_LINK
        )
    )
}