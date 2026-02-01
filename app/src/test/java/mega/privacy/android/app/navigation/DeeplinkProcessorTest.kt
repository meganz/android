package mega.privacy.android.app.navigation

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants.ACCOUNT_INVITATION_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.ALBUM_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.BUSINESS_INVITE_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.CHAT_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.CONFIRMATION_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.CONTACT_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.EXPORT_MASTER_KEY_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.FILE_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.HANDLE_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.MEGA_BLOG_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.MEGA_DROP_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.MEGA_FILE_REQUEST_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.MEGA_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.NEW_MESSAGE_CHAT_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.PASSWORD_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.PENDING_CONTACTS_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.REVERT_CHANGE_PASSWORD_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.VERIFY_CHANGE_MAIL_LINK_REGEX_ARRAY
import mega.privacy.android.app.utils.Constants.WEB_SESSION_LINK_REGEX_ARRAY
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.feature.sync.navigation.getSyncListRoute
import mega.privacy.android.feature.sync.navigation.getSyncRoute
import mega.privacy.android.navigation.DeeplinkProcessor
import org.junit.jupiter.api.Test

class DeeplinkProcessorTest {

    private val processors = setOf<@JvmSuppressWildcards DeeplinkProcessor>()

    @Test
    fun `test that the deep link processor matches sync URLs`() = runTest {
        val urls = listOf(
            "https://$MEGA_NZ_DOMAIN_NAME/${getSyncRoute()}",
            "https://$MEGA_NZ_DOMAIN_NAME/${getSyncListRoute()}",
            "https://$MEGA_APP_DOMAIN_NAME/${getSyncRoute()}",
            "https://$MEGA_APP_DOMAIN_NAME/${getSyncListRoute()}",
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
            MEGA_REGEX_ARRAY,
            FILE_LINK_REGEX_ARRAY,
            CONFIRMATION_LINK_REGEX_ARRAY,
            FOLDER_LINK_REGEX_ARRAY,
            CHAT_LINK_REGEX_ARRAY,
            PASSWORD_LINK_REGEX_ARRAY,
            ACCOUNT_INVITATION_LINK_REGEX_ARRAY,
            EXPORT_MASTER_KEY_LINK_REGEX_ARRAY,
            NEW_MESSAGE_CHAT_LINK_REGEX_ARRAY,
            CANCEL_ACCOUNT_LINK_REGEX_ARRAY,
            VERIFY_CHANGE_MAIL_LINK_REGEX_ARRAY,
            RESET_PASSWORD_LINK_REGEX_ARRAY,
            PENDING_CONTACTS_LINK_REGEX_ARRAY,
            HANDLE_LINK_REGEX_ARRAY,
            CONTACT_LINK_REGEX_ARRAY,
            MEGA_DROP_LINK_REGEX_ARRAY,
            MEGA_FILE_REQUEST_LINK_REGEX_ARRAY,
            MEGA_BLOG_LINK_REGEX_ARRAY,
            REVERT_CHANGE_PASSWORD_LINK_REGEX_ARRAY,
            EMAIL_VERIFY_LINK_REGEX_ARRAY,
            WEB_SESSION_LINK_REGEX_ARRAY,
            BUSINESS_INVITE_LINK_REGEX_ARRAY,
            ALBUM_LINK_REGEX_ARRAY
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