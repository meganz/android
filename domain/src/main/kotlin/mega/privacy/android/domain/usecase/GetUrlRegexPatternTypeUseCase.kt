package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.ACCOUNT_INVITATION_LINK
import mega.privacy.android.domain.entity.RegexPatternType.ALBUM_LINK
import mega.privacy.android.domain.entity.RegexPatternType.BUSINESS_INVITE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.CANCEL_ACCOUNT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.CHAT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.CONFIRMATION_LINK
import mega.privacy.android.domain.entity.RegexPatternType.CONTACT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.EMAIL_VERIFY_LINK
import mega.privacy.android.domain.entity.RegexPatternType.ENABLE_CAMERA_UPLOADS_LINK
import mega.privacy.android.domain.entity.RegexPatternType.EXPORT_MASTER_KEY_LINK
import mega.privacy.android.domain.entity.RegexPatternType.FILE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.FOLDER_LINK
import mega.privacy.android.domain.entity.RegexPatternType.HANDLE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.INSTALLER_DOWNLOAD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.MEGA_BLOG_LINK
import mega.privacy.android.domain.entity.RegexPatternType.MEGA_DROP_LINK
import mega.privacy.android.domain.entity.RegexPatternType.MEGA_FILE_REQUEST_LINK
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.OPEN_DEVICE_CENTER_LINK
import mega.privacy.android.domain.entity.RegexPatternType.OPEN_SYNC_MEGA_FOLDER_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PENDING_CONTACTS_LINK
import mega.privacy.android.domain.entity.RegexPatternType.PURCHASE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.RESET_PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.RESTRICTED
import mega.privacy.android.domain.entity.RegexPatternType.REVERT_CHANGE_PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.UPGRADE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.UPGRADE_PAGE_LINK
import mega.privacy.android.domain.entity.RegexPatternType.VERIFY_CHANGE_MAIL_LINK
import mega.privacy.android.domain.entity.RegexPatternType.WEB_SESSION_LINK
import mega.privacy.android.domain.entity.RegexPatternType.WHITELISTED_URL
import javax.inject.Inject

/**
 * Use Case to check if a Url matches any regex pattern listed in MEGA
 */
class GetUrlRegexPatternTypeUseCase @Inject constructor(
    private val isUrlMatchesRegexUseCase: IsUrlMatchesRegexUseCase,
    private val isUrlWhitelistedUseCase: IsUrlWhitelistedUseCase,
) {
    /**
     * Invoke
     * @param url as String
     * @return The Regex Pattern type as [RegexPatternType]
     * @see [RegexPatternType]
     */
    operator fun invoke(url: String?): RegexPatternType =
        when {
            !isUrlSanitized(url) -> RESTRICTED
            isUrlWhitelistedUseCase(url) -> WHITELISTED_URL
            isUrlMatchesRegexUseCase(url, FILE_LINK_REGEX) -> FILE_LINK
            isUrlMatchesRegexUseCase(url, CONFIRMATION_LINK_REGEX) -> CONFIRMATION_LINK
            isUrlMatchesRegexUseCase(url, FOLDER_LINK_REGEX) -> FOLDER_LINK
            isUrlMatchesRegexUseCase(url, CHAT_LINK_REGEX) -> CHAT_LINK
            isUrlMatchesRegexUseCase(url, PASSWORD_LINK_REGEX) -> PASSWORD_LINK
            isUrlMatchesRegexUseCase(url, ALBUM_LINK_REGEX) -> ALBUM_LINK
            isUrlMatchesRegexUseCase(url, ACCOUNT_INVITATION_LINK_REGEX) -> ACCOUNT_INVITATION_LINK
            isUrlMatchesRegexUseCase(url, EXPORT_MASTER_KEY_LINK_REGEX) -> EXPORT_MASTER_KEY_LINK
            isUrlMatchesRegexUseCase(url, NEW_MESSAGE_CHAT_LINK_REGEX) -> NEW_MESSAGE_CHAT_LINK
            isUrlMatchesRegexUseCase(url, CANCEL_ACCOUNT_LINK_REGEX) -> CANCEL_ACCOUNT_LINK
            isUrlMatchesRegexUseCase(url, VERIFY_CHANGE_MAIL_LINK_REGEX) -> VERIFY_CHANGE_MAIL_LINK
            isUrlMatchesRegexUseCase(url, RESET_PASSWORD_LINK_REGEX) -> RESET_PASSWORD_LINK
            isUrlMatchesRegexUseCase(url, PENDING_CONTACTS_LINK_REGEX) -> PENDING_CONTACTS_LINK
            isUrlMatchesRegexUseCase(
                url,
                OPEN_SYNC_MEGA_FOLDER_LINK_REGEX
            ) -> OPEN_SYNC_MEGA_FOLDER_LINK

            isUrlMatchesRegexUseCase(url, HANDLE_LINK_REGEX) -> HANDLE_LINK
            isUrlMatchesRegexUseCase(url, CONTACT_LINK_REGEX) -> CONTACT_LINK
            isUrlMatchesRegexUseCase(url, MEGA_DROP_LINK_REGEX) -> MEGA_DROP_LINK
            isUrlMatchesRegexUseCase(url, MEGA_FILE_REQUEST_LINK_REGEXES) -> MEGA_FILE_REQUEST_LINK
            isUrlMatchesRegexUseCase(url, MEGA_BLOG_LINK_REGEX) -> MEGA_BLOG_LINK
            isUrlMatchesRegexUseCase(url, REVERT_CHANGE_PASSWORD_LINK_REGEX) -> {
                REVERT_CHANGE_PASSWORD_LINK
            }

            isUrlMatchesRegexUseCase(url, EMAIL_VERIFY_LINK_REGEX) -> EMAIL_VERIFY_LINK
            isUrlMatchesRegexUseCase(url, WEB_SESSION_LINK_REGEX) -> WEB_SESSION_LINK
            isUrlMatchesRegexUseCase(url, BUSINESS_INVITE_LINK_REGEX) -> BUSINESS_INVITE_LINK
            isUrlMatchesRegexUseCase(url, UPGRADE_PAGE_LINK_REGEX) -> UPGRADE_PAGE_LINK
            isUrlMatchesRegexUseCase(url, INSTALLER_DOWNLOAD_LINK_REGEX) -> INSTALLER_DOWNLOAD_LINK
            isUrlMatchesRegexUseCase(url, PURCHASE_LINK_REGEX) -> PURCHASE_LINK
            isUrlMatchesRegexUseCase(url, UPGRADE_LINK_REGEX) -> UPGRADE_LINK
            isUrlMatchesRegexUseCase(
                url,
                ENABLE_CAMERA_UPLOADS_LINK_REGEX
            ) -> ENABLE_CAMERA_UPLOADS_LINK

            isUrlMatchesRegexUseCase(url, OPEN_DEVICE_CENTER_LINK_REGEX) -> OPEN_DEVICE_CENTER_LINK

            else -> RESTRICTED
        }

    private fun isUrlSanitized(url: String?) =
        !url.isNullOrBlank() &&
                (isUrlMatchesRegexUseCase(url, MEGA_REGEX) || isUrlWhitelistedUseCase(url))

    companion object {
        /**
         * This Regex Pattern will check for the existence of:
         * 1. Domain with HTTPS protocol
         * 2. Followed by either: Mega.co.nz, Mega.nz, Mega.io, Megaad.nz
         * 3. No words are allowed after the domain name, for example; <a href="https://mega.co.nzxxx">...</a> is not allowed
         * 4. Backslashes (/) or Question Mark (?) are allowed to allow path and query parameters after the MEGA domain, for example; <a href="https://mega.nz/home">...</a>
         * 5. Any characters after Backslashes (/) or Question Mark (?) are allowed, except At Sign(@)
         */
        private val MEGA_REGEX = arrayOf(
            "^https://mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz)(\\/|\\?)[^@]*$",
            "^https://([a-z0-9]+\\.)+mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz)(\\/|\\?)[^@]*$"
        )

        /**
         * This Regex Pattern checks for the existence of file path in MEGA Url
         */
        private val FILE_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#!.+$",
            "^https://mega\\.nz/.*#!.+$",
            "^https://mega\\.co\\.nz/file/.+$",
            "^https://mega\\.nz/file/.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'confirm' or '#confirm' in MEGA Url
         */
        private val CONFIRMATION_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#confirm.+$",
            "^https://mega\\.co\\.nz/.*confirm.+$",
            "^https://mega\\.nz/.*#confirm.+$",
            "^https://mega\\.nz/.*confirm.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'folder' or '#folder' in MEGA Url
         */
        private val FOLDER_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#F!.+$",
            "^https://mega\\.nz/.*#F!.+$",
            "^https://mega\\.co\\.nz/folder/.+$",
            "^https://mega\\.nz/folder/.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'chat' in MEGA Url
         */
        private val CHAT_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*chat/.+$",
            "^https://mega\\.nz/.*chat/.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'password' link in MEGA Url
         */
        private val PASSWORD_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#P!.+$",
            "^https://mega\\.nz/.*#P!.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'newsignup' link in MEGA Url
         */
        private val ACCOUNT_INVITATION_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#newsignup.+$",
            "^https://mega\\.co\\.nz/.*newsignup.+$",
            "^https://mega\\.nz/.*#newsignup.+$",
            "^https://mega\\.nz/.*newsignup.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'backup' link in MEGA Url
         */
        private val EXPORT_MASTER_KEY_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#backup",
            "^https://mega\\.nz/.*#backup"
        )

        /**
         * This Regex Pattern checks for the existence of message 'chat' link in MEGA Url
         */
        private val NEW_MESSAGE_CHAT_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#fm/chat",
            "^https://mega\\.co\\.nz/.*fm/chat",
            "^https://mega\\.nz/.*#fm/chat",
            "^https://mega\\.nz/.*fm/chat"
        )

        /**
         * This Regex Pattern checks for the existence of 'cancel' link in MEGA Url
         */
        private val CANCEL_ACCOUNT_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#cancel.+$",
            "^https://mega\\.co\\.nz/.*cancel.+$",
            "^https://mega\\.nz/.*#cancel.+$",
            "^https://mega\\.nz/.*cancel.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'verify' link in MEGA Url
         */
        private val VERIFY_CHANGE_MAIL_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#verify.+$",
            "^https://mega\\.co\\.nz/.*verify.+$",
            "^https://mega\\.nz/.*#verify.+$",
            "^https://mega\\.nz/.*verify.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'recover' link in MEGA Url
         */
        private val RESET_PASSWORD_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#recover.+$",
            "^https://mega\\.co\\.nz/.*recover.+$",
            "^https://mega\\.nz/.*#recover.+$",
            "^https://mega\\.nz/.*recover.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'ipc' link in MEGA Url
         */
        private val PENDING_CONTACTS_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#fm/ipc",
            "^https://mega\\.co\\.nz/.*fm/ipc",
            "^https://mega\\.nz/.*#fm/ipc",
            "^https://mega\\.nz/.*fm/ipc"
        )

        /**
         * This Regex Pattern checks for MEGA Url
         */
        private val HANDLE_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#.+$",
            "^https://mega\\.nz/.*#.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'contact' link in MEGA Url
         */

        private val CONTACT_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/C!.+$",
            "^https://mega\\.nz/.*C!.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'megadrop' link in MEGA Url
         */
        private val MEGA_DROP_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*megadrop/.+$",
            "^https://mega\\.nz/.*megadrop/.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'filerequest' link in MEGA Url
         */
        private val MEGA_FILE_REQUEST_LINK_REGEXES = arrayOf(
            "^https://mega\\.co\\.nz/.*filerequest/.+$",
            "^https://mega\\.nz/.*filerequest/.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'blog' link in MEGA Url
         */
        private val MEGA_BLOG_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#blog",
            "^https://mega\\.nz/.*#blog",
            "^https://mega\\.nz/.*blog",
            "^https://mega\\.co\\.nz/.*#blog.+$",
            "^https://mega\\.nz/.*#blog.+$",
            "^https://mega\\.nz/.*blog.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'pwr' link in MEGA Url
         */
        private val REVERT_CHANGE_PASSWORD_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/.*#pwr.+$",
            "^https://mega\\.co\\.nz/.*pwr.+$",
            "^https://mega\\.nz/.*#pwr.+$",
            "^https://mega\\.nz/.*pwr.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'emailverify' link in MEGA Url
         */
        private val EMAIL_VERIFY_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/#emailverify.+$",
            "^https://mega\\.co\\.nz/emailverify.+$",
            "^https://mega\\.nz/#emailverify.+$",
            "^https://mega\\.nz/emailverify.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'sitetransfer' link in MEGA Url
         */
        private val WEB_SESSION_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/#sitetransfer!.+$",
            "^https://mega\\.nz/#sitetransfer!.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'businessinvite' link in MEGA Url
         */
        private val BUSINESS_INVITE_LINK_REGEX = arrayOf(
            "^https://mega\\.co\\.nz/#businessinvite.+$",
            "^https://mega\\.co\\.nz/businessinvite.+$",
            "^https://mega\\.nz/#businessinvite.+$",
            "^https://mega\\.nz/businessinvite.+$"
        )

        /**
         * This Regex Pattern checks for the existence of 'collection/' link in MEGA Url
         */
        private val ALBUM_LINK_REGEX = arrayOf(
            "^https://mega\\.nz/collection/.+$"
        )

        /**
         * Regex pattern for upgrade page
         * Checks for a url that starts with, either:
         * - https://mega.co.nz
         * - https://mega.nz
         * Followed by /pro and then ? for query parameters or / for additional path
         * or /pro without any additional characters.
         */
        private val UPGRADE_PAGE_LINK_REGEX = arrayOf(
            "^https:\\/\\/mega(?:\\.co\\.nz|\\.nz)\\/(pro[/?].*|pro)$",
        )

        /**
         * Regex pattern for installer download link
         * Checks for a url that starts with, either:
         * - https://mega.co.nz
         * - https://mega.nz
         * Example links:
         * - https://mega.nz/MEGAvpnSetup64.exe?__hstc=254534871.de5fc61c
         * - https://mega.nz/linux/repo/xUbuntu_24.04/amd64/megacmd-xUbuntu_24.04_amd64.deb
         */
        private val INSTALLER_DOWNLOAD_LINK_REGEX = arrayOf(
            "^https:\\/\\/mega(?:\\.co\\.nz|\\.nz)\\/MEGA(.?)+\\.(exe|dmg)[?]?.*$",
            "^https:\\/\\/mega(?:\\.co\\.nz|\\.nz)\\/linux/repo/(.?)+$"
        )

        /**
         * Regex pattern for purchase link
         * Checks for a url that starts with, either:
         * - https://mega.co.nz
         * - https://mega.nz
         * Followed by /propay_1 or /propay_2 or /propay_vpn and then other characters.
         */
        private val PURCHASE_LINK_REGEX = arrayOf(
            "^https:\\/\\/mega(?:\\.co\\.nz|\\.nz)\\/propay_.+$",
        )

        /**
         * Regex for UPGRADE_LINK
         */
        private val UPGRADE_LINK_REGEX = arrayOf(
            "^https://mega\\.nz/upgrade.+\$",
            "^https://mega\\.nz/upgrade"
        )

        /**
         * This Regex Pattern checks for the existence of 'camera uploads' link in MEGA Url
         */
        private val ENABLE_CAMERA_UPLOADS_LINK_REGEX = arrayOf(
            "^https://mega\\.nz/settings/camera$"
        )

        /**
         * Regex pattern to open a Sync remote folder in Cloud Drive
         */
        private val OPEN_DEVICE_CENTER_LINK_REGEX = arrayOf(
            "^https://mega\\.nz/devicecenter"
        )

        /**
         * Regex pattern to open a Sync remote folder in Cloud Drive
         */
        private val OPEN_SYNC_MEGA_FOLDER_LINK_REGEX = arrayOf(
            "^https://mega\\.nz/opensync#.+$"
        )
    }
}