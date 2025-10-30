package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.RESTRICTED
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
    @MethodSource("validRegexType")
    fun `test that when url does match regex pattern, it should return correct regex pattern type`(
        name: String,
        urlToCheck: String?,
        pattern: RegexPatternType,
    ) {
        assertThat(underTest(urlToCheck)).isEqualTo(pattern)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidRegexType")
    fun `test that when url does not match regex pattern, it returns RESTRICTED pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isEqualTo(RESTRICTED)
    }

    private fun validRegexType(): Stream<Arguments> = Stream.of(
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
        Arguments.of("Upgrade Page", "https://mega.app/pro", RegexPatternType.UPGRADE_PAGE_LINK),
        Arguments.of(
            "Open Device Center Link",
            "https://mega.nz/devicecenter",
            RegexPatternType.OPEN_DEVICE_CENTER_LINK
        ),
        Arguments.of(
            "Open Device Center Link",
            "https://mega.app/devicecenter",
            RegexPatternType.OPEN_DEVICE_CENTER_LINK
        ),
        Arguments.of(
            "Open Sync Folder Link",
            "https://mega.nz/opensync#1234567890",
            RegexPatternType.OPEN_SYNC_MEGA_FOLDER_LINK
        ),
        Arguments.of(
            "Open Sync Folder Link",
            "https://mega.app/opensync#1234567890",
            RegexPatternType.OPEN_SYNC_MEGA_FOLDER_LINK
        ),
        Arguments.of(
            "Purchase Link",
            "https://mega.app/propay_vpn",
            RegexPatternType.PURCHASE_LINK,
        ),
        Arguments.of(
            "Download Link",
            "https://mega.app/MEGAsyncSetup64.exe",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Download Link",
            "https://mega.app/linux/repo/xUbuntu_24.04/amd64/megasync-xUbuntu_24.04_amd64.deb",
            RegexPatternType.INSTALLER_DOWNLOAD_LINK
        ),
        Arguments.of(
            "Upgrade Link",
            "https://mega.app/upgrade",
            RegexPatternType.UPGRADE_LINK
        ),
        Arguments.of(
            "Camera Uploads Link",
            "https://mega.app/settings/camera",
            RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK
        ),
        Arguments.of(
            "Login Link",
            "https://mega.nz/login",
            RegexPatternType.LOGIN_LINK
        ),
        Arguments.of(
            "Login Link",
            "https://mega.nz/login?miojid=32015f7bf6012800",
            RegexPatternType.LOGIN_LINK
        ),
        Arguments.of(
            "Login Link",
            "https://mega.app/login",
            RegexPatternType.LOGIN_LINK
        ),
        Arguments.of(
            "Login Link",
            "https://mega.app/login?miojid=32015f7bf6012800",
            RegexPatternType.LOGIN_LINK
        ),
        Arguments.of(
            "Registration Link",
            "https://mega.nz/register",
            RegexPatternType.REGISTRATION_LINK
        ),
        Arguments.of(
            "Registration Link",
            "https://mega.nz/register?miojid=32015f7bf6012800",
            RegexPatternType.REGISTRATION_LINK
        ),
        Arguments.of(
            "Registration Link",
            "https://mega.app/register",
            RegexPatternType.REGISTRATION_LINK
        ),
        Arguments.of(
            "Registration Link",
            "https://mega.app/register?miojid=32015f7bf6012800",
            RegexPatternType.REGISTRATION_LINK
        ),
    )

    private fun invalidRegexType(): Stream<Arguments> = Stream.of(
        // Invalid domains
        Arguments.of("Invalid Domain", "https://mega.com/file/abc123"),
        Arguments.of("Invalid Domain", "https://megatest.nz/file/abc123"),
        Arguments.of("Invalid Domain", "https://mega.co.com/file/abc123"),
        Arguments.of("Invalid Protocol", "http://mega.nz/file/abc123"),
        Arguments.of("Invalid Protocol", "ftp://mega.nz/file/abc123"),

        // Invalid file links
        Arguments.of("Invalid File Link - Missing Hash", "https://mega.nz/file/"),
        Arguments.of("Invalid File Link - Wrong Format", "https://mega.nz/file"),
        Arguments.of("Invalid File Link - Missing Path", "https://mega.nz/"),
        Arguments.of("Invalid File Link - Wrong Domain", "https://mega.io/file/abc123"),

        // Invalid confirmation links
        Arguments.of("Invalid Confirmation Link - Missing Parameter", "https://mega.nz/confirm"),
        Arguments.of("Invalid Confirmation Link - Wrong Format", "https://mega.nz/confirm"),
        Arguments.of("Invalid Confirmation Link - Wrong Domain", "https://mega.io/confirm/abc123"),

        // Invalid folder links
        Arguments.of("Invalid Folder Link - Missing Parameter", "https://mega.nz/folder"),
        Arguments.of("Invalid Folder Link - Wrong Format", "https://mega.nz/folder/"),
        Arguments.of("Invalid Folder Link - Wrong Hash Format", "https://mega.nz/"),
        Arguments.of("Invalid Folder Link - Wrong Domain", "https://mega.io/folder/abc123"),

        // Invalid chat links
        Arguments.of("Invalid Chat Link - Missing Parameter", "https://mega.nz/chat"),
        Arguments.of("Invalid Chat Link - Wrong Format", "https://mega.nz/chat/"),
        Arguments.of("Invalid Chat Link - Wrong Domain", "https://mega.io/chat/abc123"),

        // Invalid password links
        Arguments.of("Invalid Password Link - Missing Parameter", "https://mega.nz/password"),
        Arguments.of("Invalid Password Link - Wrong Format", "https://mega.nz/password"),
        Arguments.of("Invalid Password Link - Wrong Domain", "https://mega.io/password/abc123"),

        // Invalid account invitation links
        Arguments.of(
            "Invalid Account Invitation Link - Missing Parameter",
            "https://mega.nz/newsignup"
        ),
        Arguments.of("Invalid Account Invitation Link - Wrong Format", "https://mega.nz/newsignup"),
        Arguments.of(
            "Invalid Account Invitation Link - Wrong Domain",
            "https://mega.io/newsignup/abc123"
        ),

        // Invalid export master key links
        Arguments.of("Invalid Export Master Key Link - Missing Hash", "https://mega.nz/backup"),
        Arguments.of(
            "Invalid Export Master Key Link - Wrong Format",
            "https://mega.nz/backup/abc123"
        ),
        Arguments.of("Invalid Export Master Key Link - Wrong Domain", "https://mega.io/#backup"),

        // Invalid new message chat links
        Arguments.of("Invalid New Message Chat Link - Missing Parameter", "https://mega.nz/fm/"),
        Arguments.of("Invalid New Message Chat Link - Wrong Format", "https://mega.nz/fm/chat/"),
        Arguments.of(
            "Invalid New Message Chat Link - Wrong Domain",
            "https://mega.io/fm/chat/abc123"
        ),

        // Invalid cancel account links
        Arguments.of("Invalid Cancel Account Link - Missing Parameter", "https://mega.nz/cancel"),
        Arguments.of("Invalid Cancel Account Link - Wrong Format", "https://mega.nz/cancel"),
        Arguments.of("Invalid Cancel Account Link - Wrong Domain", "https://mega.io/cancel/abc123"),

        // Invalid verify change mail links
        Arguments.of(
            "Invalid Verify Change Mail Link - Missing Parameter",
            "https://mega.nz/verify"
        ),
        Arguments.of("Invalid Verify Change Mail Link - Wrong Format", "https://mega.nz/verify"),
        Arguments.of(
            "Invalid Verify Change Mail Link - Wrong Domain",
            "https://mega.io/verify/abc123"
        ),

        // Invalid reset password links
        Arguments.of("Invalid Reset Password Link - Missing Parameter", "https://mega.nz/recover"),
        Arguments.of("Invalid Reset Password Link - Wrong Format", "https://mega.nz/recover"),
        Arguments.of(
            "Invalid Reset Password Link - Wrong Domain",
            "https://mega.io/recover/abc123"
        ),

        // Invalid pending contacts links
        Arguments.of("Invalid Pending Contacts Link - Missing Parameter", "https://mega.nz/fm/"),
        Arguments.of("Invalid Pending Contacts Link - Wrong Format", "https://mega.nz/fm/ipc/"),
        Arguments.of(
            "Invalid Pending Contacts Link - Wrong Domain",
            "https://mega.io/fm/ipc/abc123"
        ),

        // Invalid handle links
        Arguments.of("Invalid Handle Link - Missing Hash", "https://mega.nz/"),
        Arguments.of("Invalid Handle Link - Empty Hash", "https://mega.nz/#"),
        Arguments.of("Invalid Handle Link - Wrong Domain", "https://mega.io/#abc123"),

        // Invalid contact links
        Arguments.of("Invalid Contact Link - Missing Parameter", "https://mega.nz/C!"),
        Arguments.of("Invalid Contact Link - Wrong Format", "https://mega.nz/C!"),
        Arguments.of("Invalid Contact Link - Wrong Domain", "https://mega.io/C!abc123"),

        // Invalid mega drop links
        Arguments.of("Invalid Mega Drop Link - Missing Parameter", "https://mega.nz/megadrop"),
        Arguments.of("Invalid Mega Drop Link - Wrong Format", "https://mega.nz/megadrop/"),
        Arguments.of("Invalid Mega Drop Link - Wrong Domain", "https://mega.io/megadrop/abc123"),

        // Invalid file request links
        Arguments.of(
            "Invalid File Request Link - Missing Parameter",
            "https://mega.nz/filerequest"
        ),
        Arguments.of("Invalid File Request Link - Wrong Format", "https://mega.nz/filerequest/"),
        Arguments.of(
            "Invalid File Request Link - Wrong Domain",
            "https://mega.io/filerequest/abc123"
        ),

        // Invalid blog links
        Arguments.of("Invalid Blog Link - Wrong Domain", "https://mega.io/blog/abc123"),
        Arguments.of("Invalid Blog Link - Completely Different", "https://example.com/blog"),

        // Invalid revert change password links
        Arguments.of(
            "Invalid Revert Change Password Link - Missing Parameter",
            "https://mega.nz/pwr"
        ),
        Arguments.of("Invalid Revert Change Password Link - Wrong Format", "https://mega.nz/pwr"),
        Arguments.of(
            "Invalid Revert Change Password Link - Wrong Domain",
            "https://mega.io/pwr/abc123"
        ),

        // Invalid email verify links
        Arguments.of(
            "Invalid Email Verify Link - Missing Parameter",
            "https://mega.nz/emailverify"
        ),
        Arguments.of("Invalid Email Verify Link - Wrong Format", "https://mega.nz/emailverify"),
        Arguments.of(
            "Invalid Email Verify Link - Wrong Domain",
            "https://mega.io/emailverify/abc123"
        ),

        // Invalid web session links
        Arguments.of("Invalid Web Session Link - Wrong Format", "https://mega.nz/sitetransfer"),
        Arguments.of(
            "Invalid Web Session Link - Wrong Domain",
            "https://mega.io/#sitetransfer!abc123"
        ),

        // Invalid business invite links
        Arguments.of(
            "Invalid Business Invite Link - Missing Parameter",
            "https://mega.nz/businessinvite"
        ),
        Arguments.of(
            "Invalid Business Invite Link - Wrong Format",
            "https://mega.nz/businessinvite"
        ),
        Arguments.of(
            "Invalid Business Invite Link - Wrong Domain",
            "https://mega.io/businessinvite/abc123"
        ),

        // Invalid album links
        Arguments.of("Invalid Album Link - Missing Parameter", "https://mega.nz/collection"),
        Arguments.of("Invalid Album Link - Wrong Format", "https://mega.nz/collection/"),
        Arguments.of("Invalid Album Link - Wrong Domain", "https://mega.co.nz/collection/abc123"),

        // Invalid upgrade page links
        Arguments.of("Invalid Upgrade Page Link - Wrong Domain", "https://mega.io/pro"),
        Arguments.of("Invalid Upgrade Page Link - Wrong Path", "https://mega.nz/notpro"),
        Arguments.of(
            "Invalid Upgrade Page Link - Completely Different",
            "https://mega.nz/invalidpath"
        ),
        Arguments.of(
            "Invalid Upgrade Page Link - Completely Different",
            "https://mega.nz/randompath"
        ),
        Arguments.of("Invalid Upgrade Page Link - Completely Different", "https://mega.nz/xyz"),
        Arguments.of("Invalid Upgrade Page Link - Completely Different", "https://mega.nz/123"),

        // Invalid installer download links
        Arguments.of(
            "Invalid Installer Download Link - Wrong Extension",
            "https://mega.nz/MEGAsyncSetup.zip"
        ),
        Arguments.of(
            "Invalid Installer Download Link - Wrong Path",
            "https://mega.nz/download/MEGAsyncSetup64.exe"
        ),
        Arguments.of(
            "Invalid Installer Download Link - Wrong Domain",
            "https://mega.io/MEGAsyncSetup64.exe"
        ),
        Arguments.of(
            "Invalid Installer Download Link - Wrong Linux Path",
            "https://mega.nz/linux/download/megasync.deb"
        ),

        // Invalid purchase links
        Arguments.of("Invalid Purchase Link - Missing Parameter", "https://mega.nz/propay_"),
        Arguments.of("Invalid Purchase Link - Wrong Format", "https://mega.nz/propay"),
        Arguments.of("Invalid Purchase Link - Wrong Domain", "https://mega.io/propay_1"),

        // Invalid upgrade links
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.co.nz/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.io/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Path", "https://mega.nz/notupgrade"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/invalidpath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/randompath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/xyz"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/123"),

        // Invalid camera uploads links
        Arguments.of(
            "Invalid Camera Uploads Link - Wrong Domain",
            "https://mega.co.nz/settings/camera"
        ),
        Arguments.of(
            "Invalid Camera Uploads Link - Wrong Domain",
            "https://mega.io/settings/camera"
        ),
        Arguments.of(
            "Invalid Camera Uploads Link - Extra Path",
            "https://mega.nz/settings/camera/extra"
        ),

        // Invalid device center links
        Arguments.of(
            "Invalid Device Center Link - Wrong Domain",
            "https://mega.co.nz/devicecenter"
        ),
        Arguments.of("Invalid Device Center Link - Wrong Domain", "https://mega.io/devicecenter"),
        Arguments.of(
            "Invalid Device Center Link - Extra Path",
            "https://mega.nz/devicecenter/extra"
        ),

        // Invalid open sync folder links
        Arguments.of("Invalid Open Sync Folder Link - Missing Hash", "https://mega.nz/opensync"),
        Arguments.of("Invalid Open Sync Folder Link - Empty Hash", "https://mega.nz/opensync#"),
        Arguments.of(
            "Invalid Open Sync Folder Link - Wrong Domain",
            "https://mega.io/opensync#abc123"
        ),

        // Invalid URLs with @ symbol (should be restricted)
        Arguments.of("URL with @ symbol", "https://mega.nz/file/abc@123"),
        Arguments.of("URL with @ symbol", "https://mega.nz/folder/abc@123"),
        Arguments.of("URL with @ symbol", "https://mega.nz/chat/abc@123"),

        // Invalid URLs with wrong protocol
        Arguments.of("HTTP instead of HTTPS", "http://mega.nz/file/abc123"),
        Arguments.of("FTP instead of HTTPS", "ftp://mega.nz/file/abc123"),

        // Invalid URLs with extra characters in domain
        Arguments.of("Extra characters after domain", "https://mega.nzxxx/file/abc123"),
        Arguments.of("Extra characters after domain", "https://mega.coz.nz/notfile/abc123"),
        Arguments.of("Extra characters after domain", "https://mega.ios/invalidpath/abc123"),
        Arguments.of("Extra characters after domain", "https://mega.apps/randompath/abc123"),
        Arguments.of("Extra characters after domain", "https://mega.co.nzs/xyz/abc123"),
        Arguments.of("Extra characters after domain", "https://mega.nz/123/abc123"),

        // Null and empty URLs
        Arguments.of("Null URL", null),
        Arguments.of("Empty URL", ""),
        Arguments.of("Blank URL", "   "),

        // Completely invalid URLs
        Arguments.of("Completely invalid URL", "not-a-url"),
        Arguments.of("Completely invalid URL", "mega.nz"),
        Arguments.of("Completely invalid URL", "https://"),
        // Invalid login links
        Arguments.of(
            "Invalid Login Link - Wrong Domain",
            "https://mega.co.nz/login"
        ),
        Arguments.of(
            "Invalid Login Link - Extra characters after domain",
            "https://mega.nz/alogin"
        ),
        Arguments.of(
            "Invalid Login Link - Extra characters after domain",
            "https://mega.app/alogin/asfsa"
        ),
        // Invalid registration links
        Arguments.of(
            "Invalid Login Link - Wrong Domain",
            "https://mega.co.nz/register"
        ),
        Arguments.of(
            "Invalid Login Link - Extra characters after domain",
            "https://mega.nz/aregister/asfsa"
        ),
        Arguments.of(
            "Invalid Login Link - Extra characters after domain",
            "https://mega.app/aregister/asfsa"
        ),
    )
}