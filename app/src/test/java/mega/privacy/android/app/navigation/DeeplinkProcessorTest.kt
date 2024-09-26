package mega.privacy.android.app.navigation

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants.ACCOUNT_INVITATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.ALBUM_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.BUSINESS_INVITE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CHAT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CONFIRMATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CONTACT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.EXPORT_MASTER_KEY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.HANDLE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.MEGA_BLOG_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.MEGA_DROP_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.MEGA_FILE_REQUEST_LINK_REGEXES
import mega.privacy.android.app.utils.Constants.MEGA_REGEXS
import mega.privacy.android.app.utils.Constants.NEW_MESSAGE_CHAT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.PENDING_CONTACTS_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.REVERT_CHANGE_PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.WEB_SESSION_LINK_REGEXS
import mega.privacy.android.feature.sync.navigation.getSyncListRoute
import mega.privacy.android.feature.sync.navigation.getSyncRoute
import mega.privacy.android.navigation.DeeplinkProcessor
import org.junit.jupiter.api.Test

class DeeplinkProcessorTest {

    private val processors = setOf<@JvmSuppressWildcards DeeplinkProcessor>()

    @Test
    fun `test that the deep link processor matches sync URLs`() = runTest {
        val urls = listOf(
            "https://mega.nz/${getSyncRoute()}",
            "https://mega.nz/${getSyncListRoute()}",
        )
        processors.forEach { processor ->
            urls.forEach { url ->
                assert(processor.matches(url))
            }
        }
    }

    @Test
    fun `test that the deep link processor doesn't match other URLs`() = runTest {

        val regexList = listOf(
            MEGA_REGEXS,
            FILE_LINK_REGEXS,
            CONFIRMATION_LINK_REGEXS,
            FOLDER_LINK_REGEXS,
            CHAT_LINK_REGEXS,
            PASSWORD_LINK_REGEXS,
            ACCOUNT_INVITATION_LINK_REGEXS,
            EXPORT_MASTER_KEY_LINK_REGEXS,
            NEW_MESSAGE_CHAT_LINK_REGEXS,
            CANCEL_ACCOUNT_LINK_REGEXS,
            VERIFY_CHANGE_MAIL_LINK_REGEXS,
            RESET_PASSWORD_LINK_REGEXS,
            PENDING_CONTACTS_LINK_REGEXS,
            HANDLE_LINK_REGEXS,
            CONTACT_LINK_REGEXS,
            MEGA_DROP_LINK_REGEXS,
            MEGA_FILE_REQUEST_LINK_REGEXES,
            MEGA_BLOG_LINK_REGEXS,
            REVERT_CHANGE_PASSWORD_LINK_REGEXS,
            EMAIL_VERIFY_LINK_REGEXS,
            WEB_SESSION_LINK_REGEXS,
            BUSINESS_INVITE_LINK_REGEXS,
            ALBUM_LINK_REGEXS
        )

        processors.forEach { processor ->
            regexList.forEach { regex ->
                regex.forEach { url ->
                    assert(!processor.matches(url))
                }
            }
        }
    }
}