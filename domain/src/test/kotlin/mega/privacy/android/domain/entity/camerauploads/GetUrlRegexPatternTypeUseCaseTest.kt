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

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidFileLinks")
    fun `test that when url does not match file link pattern, it does not return FILE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.FILE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidFolderLinks")
    fun `test that when url does not match folder link pattern, it does not return FOLDER_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.FOLDER_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidChatLinks")
    fun `test that when url does not match chat link pattern, it does not return CHAT_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.CHAT_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidConfirmationLinks")
    fun `test that when url does not match confirmation link pattern, it does not return CONFIRMATION_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.CONFIRMATION_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidPasswordLinks")
    fun `test that when url does not match password link pattern, it does not return CONFIRMATION_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.PASSWORD_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidAccountInvitationLinks")
    fun `test that when url does not match account invitation link pattern, it does not return ACCOUNT_INVITATION_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.ACCOUNT_INVITATION_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidExportMasterKeyLinks")
    fun `test that when url does not match export master key link pattern, it does not return EXPORT_MASTER_KEY_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.EXPORT_MASTER_KEY_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidNewMessageLinks")
    fun `test that when url does not match new message link pattern, it does not return NEW_MESSAGE_CHAT_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.NEW_MESSAGE_CHAT_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidCancelAccountLinks")
    fun `test that when url does not match cancel account link pattern, it does not return CANCEL_ACCOUNT_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.CANCEL_ACCOUNT_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidChangeEmailLinks")
    fun `test that when url does not match change email link pattern, it does not return VERIFY_CHANGE_MAIL_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.VERIFY_CHANGE_MAIL_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidResetPasswordLinks")
    fun `test that when url does not match reset password link pattern, it does not return RESET_PASSWORD_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.RESET_PASSWORD_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidPendingContactLinks")
    fun `test that when url does not match pending contacts link pattern, it does not return PENDING_CONTACTS_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.PENDING_CONTACTS_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidHandleLinks")
    fun `test that when url does not match handle link pattern, it does not return HANDLE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.HANDLE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidContactLinks")
    fun `test that when url does not match contact link pattern, it does not return CONTACT_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.CONTACT_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidMegaDropLinks")
    fun `test that when url does not match mega drop link pattern, it does not return MEGA_DROP_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.MEGA_DROP_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidFileRequestLinks")
    fun `test that when url does not match file request link pattern, it does not return MEGA_FILE_REQUEST_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.MEGA_FILE_REQUEST_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidBlogLinks")
    fun `test that when url does not match blog link pattern, it does not return MEGA_BLOG_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.MEGA_BLOG_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidRevertChangePasswordLinks")
    fun `test that when url does not match revert change password link pattern, it does not return REVERT_CHANGE_PASSWORD_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.REVERT_CHANGE_PASSWORD_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidEmailVerifyLinks")
    fun `test that when url does not match email verify link pattern, it does not return EMAIL_VERIFY_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.EMAIL_VERIFY_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidWebSessionLinks")
    fun `test that when url does not match web session link pattern, it does not return WEB_SESSION_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.WEB_SESSION_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidBusinessInviteLinks")
    fun `test that when url does not match business invite link pattern, it does not return BUSINESS_INVITE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.BUSINESS_INVITE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidAlbumLinks")
    fun `test that when url does not match album link pattern, it does not return ALBUM_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.ALBUM_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidUpgradePageLinks")
    fun `test that when url does not match upgrade page link pattern, it does not return UPGRADE_PAGE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.UPGRADE_PAGE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidInstallerDownloadsLinks")
    fun `test that when url does not match installer download link pattern, it does not return INSTALLER_DOWNLOAD_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.INSTALLER_DOWNLOAD_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidPurchaseLinks")
    fun `test that when url does not match purchase link pattern, it does not return PURCHASE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.PURCHASE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidUpgradeLinks")
    fun `test that when url does not match upgrade link pattern, it does not return UPGRADE_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.UPGRADE_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidEnableCameraUploadsLinks")
    fun `test that when url does not match camera uploads link pattern, it does not return ENABLE_CAMERA_UPLOADS_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidDeviceCenterLinks")
    fun `test that when url does not match device center link pattern, it does not return OPEN_DEVICE_CENTER_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.OPEN_DEVICE_CENTER_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidLoginLinks")
    fun `test that when url does not match login link pattern, it does not return LOGIN_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.LOGIN_LINK)
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidRegistrationLinks")
    fun `test that when url does not match register link pattern, it does not return REGISTRATION_LINK pattern type`(
        name: String,
        urlToCheck: String?,
    ) {
        assertThat(underTest(urlToCheck)).isNotEqualTo(RegexPatternType.REGISTRATION_LINK)
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
        Arguments.of(
            "MEGA Link",
            "https://mega.app/whatever",
            RegexPatternType.MEGA_LINK
        ),
        Arguments.of(
            "MEGA Link",
            "https://mega.nz/whatever",
            RegexPatternType.MEGA_LINK
        ),
        Arguments.of(
            "MEGA Link",
            "https://mega.co.nz/whatever",
            RegexPatternType.MEGA_LINK
        ),
        Arguments.of(
            "MEGA Link",
            "https://mega.io/whatever",
            RegexPatternType.MEGA_LINK
        ),
    )

    private fun invalidFileLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid File Link - Missing Hash", "https://mega.nz/file/"),
        Arguments.of("Invalid File Link - Wrong Format", "https://mega.nz/file"),
        Arguments.of("Invalid File Link - Missing Path", "https://mega.nz/"),
        Arguments.of("Invalid File Link - Wrong Domain", "https://mega.io/file/abc123"),
    )

    private fun invalidFolderLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Folder Link - Missing Parameter", "https://mega.nz/folder"),
        Arguments.of("Invalid Folder Link - Wrong Format", "https://mega.nz/folder/"),
        Arguments.of("Invalid Folder Link - Wrong Hash Format", "https://mega.nz/"),
        Arguments.of("Invalid Folder Link - Wrong Domain", "https://mega.io/folder/abc123"),
    )

    private fun invalidChatLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Chat Link - Missing Parameter", "https://mega.nz/chat"),
        Arguments.of("Invalid Chat Link - Wrong Format", "https://mega.nz/chat/"),
        Arguments.of("Invalid Chat Link - Wrong Domain", "https://mega.io/chat/abc123"),
    )

    private fun invalidConfirmationLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Confirmation Link - Missing Parameter", "https://mega.nz/confirm"),
        Arguments.of("Invalid Confirmation Link - Wrong Format", "https://mega.nz/confirm"),
        Arguments.of("Invalid Confirmation Link - Wrong Domain", "https://mega.io/confirm/abc123"),
    )

    private fun invalidPasswordLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Password Link - Missing Parameter", "https://mega.nz/password"),
        Arguments.of("Invalid Password Link - Wrong Format", "https://mega.nz/password"),
        Arguments.of("Invalid Password Link - Wrong Domain", "https://mega.io/password/abc123"),
    )

    private fun invalidAccountInvitationLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Account Invitation Link - Missing Parameter",
            "https://mega.nz/newsignup"
        ),
        Arguments.of("Invalid Account Invitation Link - Wrong Format", "https://mega.nz/newsignup"),
        Arguments.of(
            "Invalid Account Invitation Link - Wrong Domain",
            "https://mega.io/newsignup/abc123"
        ),
    )

    private fun invalidExportMasterKeyLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Export Master Key Link - Missing Hash", "https://mega.nz/backup"),
        Arguments.of(
            "Invalid Export Master Key Link - Wrong Format",
            "https://mega.nz/backup/abc123"
        ),
        Arguments.of("Invalid Export Master Key Link - Wrong Domain", "https://mega.io/#backup"),
    )

    private fun invalidNewMessageLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid New Message Chat Link - Missing Parameter", "https://mega.nz/fm/"),
        Arguments.of("Invalid New Message Chat Link - Wrong Format", "https://mega.nz/fm/chat/"),
        Arguments.of(
            "Invalid New Message Chat Link - Wrong Domain",
            "https://mega.io/fm/chat/abc123"
        ),
    )

    private fun invalidCancelAccountLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Cancel Account Link - Missing Parameter", "https://mega.nz/cancel"),
        Arguments.of("Invalid Cancel Account Link - Wrong Format", "https://mega.nz/cancel"),
        Arguments.of("Invalid Cancel Account Link - Wrong Domain", "https://mega.io/cancel/abc123"),
    )

    private fun invalidChangeEmailLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Verify Change Mail Link - Missing Parameter",
            "https://mega.nz/verify"
        ),
        Arguments.of("Invalid Verify Change Mail Link - Wrong Format", "https://mega.nz/verify"),
        Arguments.of(
            "Invalid Verify Change Mail Link - Wrong Domain",
            "https://mega.io/verify/abc123"
        ),
    )

    private fun invalidResetPasswordLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Reset Password Link - Missing Parameter", "https://mega.nz/recover"),
        Arguments.of("Invalid Reset Password Link - Wrong Format", "https://mega.nz/recover"),
        Arguments.of(
            "Invalid Reset Password Link - Wrong Domain",
            "https://mega.io/recover/abc123"
        ),
        Arguments.of(
            "Invalid Reset Password Link - Wrong Domain",
            "https://mega.app/recovery"
        ),
        Arguments.of(
            "Invalid Reset Password Link - Wrong Domain",
            "https://mega.nz/recovery"
        ),
    )

    private fun invalidPendingContactLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Pending Contacts Link - Missing Parameter", "https://mega.nz/fm/"),
        Arguments.of("Invalid Pending Contacts Link - Wrong Format", "https://mega.nz/fm/ipc/"),
        Arguments.of(
            "Invalid Pending Contacts Link - Wrong Domain",
            "https://mega.io/fm/ipc/abc123"
        ),
    )

    private fun invalidHandleLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Handle Link - Missing Hash", "https://mega.nz/"),
        Arguments.of("Invalid Handle Link - Empty Hash", "https://mega.nz/#"),
        Arguments.of("Invalid Handle Link - Wrong Domain", "https://mega.io/#abc123"),
    )

    private fun invalidContactLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Contact Link - Missing Parameter", "https://mega.nz/C!"),
        Arguments.of("Invalid Contact Link - Wrong Format", "https://mega.nz/C!"),
        Arguments.of("Invalid Contact Link - Wrong Domain", "https://mega.io/C!abc123"),
    )

    private fun invalidMegaDropLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Mega Drop Link - Missing Parameter", "https://mega.nz/megadrop"),
        Arguments.of("Invalid Mega Drop Link - Wrong Format", "https://mega.nz/megadrop/"),
        Arguments.of("Invalid Mega Drop Link - Wrong Domain", "https://mega.io/megadrop/abc123"),
    )

    private fun invalidFileRequestLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid File Request Link - Missing Parameter",
            "https://mega.nz/filerequest"
        ),
        Arguments.of("Invalid File Request Link - Wrong Format", "https://mega.nz/filerequest/"),
        Arguments.of(
            "Invalid File Request Link - Wrong Domain",
            "https://mega.io/filerequest/abc123"
        ),
    )

    private fun invalidBlogLinks(): Stream<Arguments> = Stream.of(
        // Invalid blog links
        Arguments.of("Invalid Blog Link - Wrong Domain", "https://mega.io/blog/abc123"),
        Arguments.of("Invalid Blog Link - Completely Different", "https://example.com/blog"),
    )

    private fun invalidRevertChangePasswordLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Revert Change Password Link - Missing Parameter",
            "https://mega.nz/pwr"
        ),
        Arguments.of("Invalid Revert Change Password Link - Wrong Format", "https://mega.nz/pwr"),
        Arguments.of(
            "Invalid Revert Change Password Link - Wrong Domain",
            "https://mega.io/pwr/abc123"
        ),
    )

    private fun invalidEmailVerifyLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Email Verify Link - Missing Parameter",
            "https://mega.nz/emailverify"
        ),
        Arguments.of("Invalid Email Verify Link - Wrong Format", "https://mega.nz/emailverify"),
        Arguments.of(
            "Invalid Email Verify Link - Wrong Domain",
            "https://mega.io/emailverify/abc123"
        ),
    )

    private fun invalidWebSessionLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Web Session Link - Wrong Format", "https://mega.nz/sitetransfer"),
        Arguments.of(
            "Invalid Web Session Link - Wrong Domain",
            "https://mega.io/#sitetransfer!abc123"
        ),
    )

    private fun invalidBusinessInviteLinks(): Stream<Arguments> = Stream.of(
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
    )

    private fun invalidAlbumLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Album Link - Missing Parameter", "https://mega.nz/collection"),
        Arguments.of("Invalid Album Link - Wrong Format", "https://mega.nz/collection/"),
        Arguments.of("Invalid Album Link - Wrong Domain", "https://mega.co.nz/collection/abc123"),
    )

    private fun invalidUpgradePageLinks(): Stream<Arguments> = Stream.of(
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
    )

    private fun invalidInstallerDownloadsLinks(): Stream<Arguments> = Stream.of(
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
    )

    private fun invalidPurchaseLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Purchase Link - Missing Parameter", "https://mega.nz/propay_"),
        Arguments.of("Invalid Purchase Link - Wrong Format", "https://mega.nz/propay"),
        Arguments.of("Invalid Purchase Link - Wrong Domain", "https://mega.io/propay_1"),
    )

    private fun invalidUpgradeLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.co.nz/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.io/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Path", "https://mega.nz/notupgrade"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/invalidpath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/randompath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/xyz"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/123"),
    )

    private fun invalidEnableCameraUploadsLinks(): Stream<Arguments> = Stream.of(
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.co.nz/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Domain", "https://mega.io/upgrade"),
        Arguments.of("Invalid Upgrade Link - Wrong Path", "https://mega.nz/notupgrade"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/invalidpath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/randompath"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/xyz"),
        Arguments.of("Invalid Upgrade Link - Completely Different", "https://mega.nz/123"),
    )

    private fun invalidDeviceCenterLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Device Center Link - Wrong Domain",
            "https://mega.co.nz/devicecenter"
        ),
        Arguments.of("Invalid Device Center Link - Wrong Domain", "https://mega.io/devicecenter"),
        Arguments.of(
            "Invalid Device Center Link - Extra Path",
            "https://mega.nz/devicecenter/extra"
        ),
    )

    private fun invalidLoginLinks(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "Invalid Login Link - Wrong Domain",
            "https://mega.io/laogin"
        ),
        Arguments.of(
            "Invalid Login Link - Wrong Domain",
            "https://mega.co.nz/laogin"
        ),
        Arguments.of(
            "Invalid Login Link - Wrong Domain",
            "https://mega.co.nz/login"
        ),
    )

    private fun invalidRegistrationLinks(): Stream<Arguments> = Stream.of(
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

    private fun invalidRegexType(): Stream<Arguments> = Stream.of(
        // Invalid domains
        Arguments.of("Invalid Domain", "https://mega.com/file/abc123"),
        Arguments.of("Invalid Domain", "https://megatest.nz/file/abc123"),
        Arguments.of("Invalid Domain", "https://mega.co.com/file/abc123"),
        Arguments.of("Invalid Protocol", "http://mega.nz/file/abc123"),
        Arguments.of("Invalid Protocol", "ftp://mega.nz/file/abc123"),

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

        // Null and empty URLs
        Arguments.of("Null URL", null),
        Arguments.of("Empty URL", ""),
        Arguments.of("Blank URL", "   "),

        // Completely invalid URLs
        Arguments.of("Completely invalid URL", "not-a-url"),
        Arguments.of("Completely invalid URL", "mega.nz"),
        Arguments.of("Completely invalid URL", "https://"),
    )
}