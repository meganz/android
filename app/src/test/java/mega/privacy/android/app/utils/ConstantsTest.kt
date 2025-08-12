package mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.app.utils.Util.matchRegexs
import org.junit.Test

/**
 * Unit tests for Constants class.
 */
class ConstantsTest {
    /**
     * test Regex of Constants.FILE_LINK_REGEXS
     */
    @Test
    fun test_Constants_FILE_LINK_REGEXS() {
        // Test mega.co.nz
        assertThat(matchRegexs("https://mega.co.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/file/abc", FILE_LINK_REGEXS)).isTrue()

        // Test mega.nz
        assertThat(matchRegexs("https://mega.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/file/abc#!def", FILE_LINK_REGEXS)).isTrue()

        // Test mega.app
        assertThat(matchRegexs("https://mega.app/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.app/#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.app/file/abc", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.app/file/abc#!def", FILE_LINK_REGEXS)).isTrue()

        // Test invalid domains
        assertThat(matchRegexs("https://mega.io/abc#!def", FILE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.com/abc#!def", FILE_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.CONFIRMATION_LINK_REGEXS
     */
    @Test
    fun test_Constants_CONFIRMATION_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#confirmxxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#confirmdxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxconfirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#confirmxxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#confirmdxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxconfirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#confirmxxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#confirmdxxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*confirm.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxconfirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#confirmxxxx",
                CONFIRMATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/confirm",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                CONFIRMATION_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.ACCOUNT_INVITATION_LINK_REGEXS
     */
    @Test
    fun test_Constants_ACCOUNT_INVITATION_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#newsignupxxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#newsignupdxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxnewsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#newsignupxxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#newsignupdxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxnewsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#newsignupxxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#newsignupdxxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*newsignup.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxnewsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#newsignupxxxx",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/newsignup",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                ACCOUNT_INVITATION_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.NEW_MESSAGE_CHAT_LINK_REGEXS
     */
    @Test
    fun test_Constants_NEW_MESSAGE_CHAT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxx#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/chatxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/CHATxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxfm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/fm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxfm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nzxx/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.nz/#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxx#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#fm/chatxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.cnz/#fm/CHATxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.nz/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxfm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/fm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxfm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nzxx/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxx#fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/chatxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/CHATxxx",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*fm/chat"
        assertThat(
            matchRegexs(
                "https://mega.app/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxfm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/fm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxfm/chatXXX",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.appxx/fm/chat",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                NEW_MESSAGE_CHAT_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.CANCEL_ACCOUNT_LINK_REGEXS
     */
    @Test
    fun test_Constants_CANCEL_ACCOUNT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#cancelxxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#canceldxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()


        // test "^https://mega\\.co\\.nz/.*cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxcancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#cancelxxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#canceldxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#cancelQIsyQY4BAc8QBCyAwUt03YqB",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxcancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#cancelxxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#canceldxxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*cancel.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxcancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#cancelxxxx",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/cancel",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                CANCEL_ACCOUNT_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS
     */
    @Test
    fun test_Constants_VERIFY_CHANGE_MAIL_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#verifyxxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#verifydxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxverifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#verifyxxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#verifydxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxverifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#verifyxxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#verifydxxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*verify.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxverifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#verifyxxxx",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/verify",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                VERIFY_CHANGE_MAIL_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.RESET_PASSWORD_LINK_REGEXS
     */
    @Test
    fun test_Constants_RESET_PASSWORD_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#recoverxxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#recoverdxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxrecoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#recoverxxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#recoverdxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxrecoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#recoverxxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#recoverdxxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*recover.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxrecoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#recoverxxxx",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/recover",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                RESET_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.PENDING_CONTACTS_LINK_REGEXS
     */
    @Test
    fun test_Constants_PENDING_CONTACTS_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxx#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/ipcxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#fm/IPCxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxfm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/fm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxfm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nzxx/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nzxx/fm/IPC",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.nz/#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxx#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#fm/ipcxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.cnz/#fm/CHATxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.nz/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxfm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/fm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxfm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nzxx/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nzxx/fm/IPC",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxx#fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/ipcxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/#fm/IPCxxx",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*fm/ipc"
        assertThat(
            matchRegexs(
                "https://mega.app/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxfm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/fm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxfm/ipcXXX",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.appxx/fm/ipc",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.appxx/fm/IPC",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                PENDING_CONTACTS_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.EMAIL_VERIFY_LINK_REGEXS
     */
    @Test
    fun test_Constants_EMAIL_VERIFY_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/#emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#emailverifyxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#emailverifyxxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/emailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxemailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxemailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/emailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/#emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/#emailverifyxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#emailverifyxxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/emailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxemailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxemailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/emailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/#emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/#emailverifyxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#emailverifyxxxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/emailverify.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/emailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/emailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxemailverifyxxxx",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxemailverify",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                EMAIL_VERIFY_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.BUSINESS_INVITE_LINK_REGEXS
     */
    @Test
    fun test_Constants_BUSINESS_INVITE_LINK_REGEXS() {

        // test "^https://mega\\.co\\.nz/#businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#businessinvitexxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#businessinvitexxxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/businessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxbusinessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/xxxbusinessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/businessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/#businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/#businessinvitexxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#businessinviteL6NfzzIHAAcQegDg7Uko",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#businessinvitexxxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/businessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/businessinviteL6NfzzIHAAcQegDg7Uko",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxbusinessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.nz/xxxbusinessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.io/businessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/#businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/#businessinvitexxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#businessinvitexxxxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/businessinvite.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/businessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/businessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxbusinessinvitexxxx",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
        assertThat(
            matchRegexs(
                "https://mega.app/xxxbusinessinvite",
                BUSINESS_INVITE_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.MEGA_REGEXS
     */
    @Test
    fun test_Constants_MEGA_REGEXS() {
        // test "^https://mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz|\\.app)(\\/|\\?)[^@]*$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/?param=value",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/?param=value",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/?param=value",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz@invalid",
                MEGA_REGEXS
            )
        ).isFalse()

        // test "^https://([a-z0-9]+\\.)+mega(?:\\.co\\.nz|\\.nz|\\.io|ad\\.nz|\\.app)(\\/|\\?)[^@]*$"
        assertThat(
            matchRegexs(
                "https://subdomain.mega.co.nz/",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://subdomain.mega.nz/",
                MEGA_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://subdomain.mega.app/",
                MEGA_REGEXS
            )
        ).isTrue()

        assertThat(
            matchRegexs(
                "",
                MEGA_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.FOLDER_LINK_REGEXS
     */
    @Test
    fun test_Constants_FOLDER_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#F!.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#F!def",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#F!",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/folder/.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/folder/abc",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/folder/",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#F!.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#F!def",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#F!",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/folder/.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/folder/abc",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/folder/",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#F!.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#F!def",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#F!",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/folder/.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/folder/abc",
                FOLDER_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/folder/",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                FOLDER_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.CHAT_LINK_REGEXS
     */
    @Test
    fun test_Constants_CHAT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*chat/.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/chat/abc",
                CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/chat/",
                CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*chat/.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/chat/abc",
                CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/chat/",
                CHAT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*chat/.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/chat/abc",
                CHAT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/chat/",
                CHAT_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                CHAT_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.PASSWORD_LINK_REGEXS
     */
    @Test
    fun test_Constants_PASSWORD_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#P!.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#P!def",
                PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#P!",
                PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#P!.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#P!def",
                PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#P!",
                PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#P!.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#P!def",
                PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#P!",
                PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                PASSWORD_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.EXPORT_MASTER_KEY_LINK_REGEXS
     */
    @Test
    fun test_Constants_EXPORT_MASTER_KEY_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#backup"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#backup",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#backupxxx",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#backup"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#backup",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#backupxxx",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#backup"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#backup",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#backupxxx",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                EXPORT_MASTER_KEY_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.HANDLE_LINK_REGEXS
     */
    @Test
    fun test_Constants_HANDLE_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#def",
                HANDLE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#",
                HANDLE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#def",
                HANDLE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#",
                HANDLE_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#def",
                HANDLE_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#",
                HANDLE_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                HANDLE_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.CONTACT_LINK_REGEXS
     */
    @Test
    fun test_Constants_CONTACT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/C!.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/C!def",
                CONTACT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/C!",
                CONTACT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*C!.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/C!def",
                CONTACT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/C!",
                CONTACT_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*C!.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/C!def",
                CONTACT_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/C!",
                CONTACT_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                CONTACT_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.MEGA_DROP_LINK_REGEXS
     */
    @Test
    fun test_Constants_MEGA_DROP_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*megadrop/.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/megadrop/abc",
                MEGA_DROP_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/megadrop/",
                MEGA_DROP_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*megadrop/.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/megadrop/abc",
                MEGA_DROP_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/megadrop/",
                MEGA_DROP_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*megadrop/.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/megadrop/abc",
                MEGA_DROP_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/megadrop/",
                MEGA_DROP_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                MEGA_DROP_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.MEGA_FILE_REQUEST_LINK_REGEXES
     */
    @Test
    fun test_Constants_MEGA_FILE_REQUEST_LINK_REGEXES() {
        // test "^https://mega\\.co\\.nz/.*filerequest/.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/filerequest/abc",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/filerequest/",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*filerequest/.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/filerequest/abc",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/filerequest/",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isFalse()

        // test "^https://mega\\.app/.*filerequest/.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/filerequest/abc",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/filerequest/",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                MEGA_FILE_REQUEST_LINK_REGEXES
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.MEGA_BLOG_LINK_REGEXS
     */
    @Test
    fun test_Constants_MEGA_BLOG_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#blog"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.co\\.nz/.*#blog.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.nz/.*#blog"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.nz/.*blog"
        assertThat(
            matchRegexs(
                "https://mega.nz/blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.nz/.*#blog.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.nz/.*blog.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.app/.*#blog"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.app/.*blog"
        assertThat(
            matchRegexs(
                "https://mega.app/blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.app/.*#blog.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // test "^https://mega\\.app/.*blog.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isTrue()

        // Negative tests - URLs that should NOT match
        assertThat(
            matchRegexs(
                "https://mega.co.nz/blog",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "https://mega.co.nz/blogxxxx",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                MEGA_BLOG_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.REVERT_CHANGE_PASSWORD_LINK_REGEXS
     */
    @Test
    fun test_Constants_REVERT_CHANGE_PASSWORD_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/abc#pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.co\\.nz/.*pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*#pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/abc#pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/.*pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*#pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/abc#pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/abc#pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/.*pwr.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/pwrxxxx",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/pwr",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                REVERT_CHANGE_PASSWORD_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.WEB_SESSION_LINK_REGEXS
     */
    @Test
    fun test_Constants_WEB_SESSION_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/#sitetransfer!.+$"
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#sitetransfer!def",
                WEB_SESSION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.co.nz/#sitetransfer!",
                WEB_SESSION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.nz/#sitetransfer!.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/#sitetransfer!def",
                WEB_SESSION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/#sitetransfer!",
                WEB_SESSION_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/#sitetransfer!.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/#sitetransfer!def",
                WEB_SESSION_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/#sitetransfer!",
                WEB_SESSION_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                WEB_SESSION_LINK_REGEXS
            )
        ).isFalse()
    }

    /**
     * test Regex of Constants.ALBUM_LINK_REGEXS
     */
    @Test
    fun test_Constants_ALBUM_LINK_REGEXS() {
        // test "^https://mega\\.nz/collection/.+$"
        assertThat(
            matchRegexs(
                "https://mega.nz/collection/abc",
                ALBUM_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.nz/collection/",
                ALBUM_LINK_REGEXS
            )
        ).isFalse()

        // test "^https://mega\\.app/collection/.+$"
        assertThat(
            matchRegexs(
                "https://mega.app/collection/abc",
                ALBUM_LINK_REGEXS
            )
        ).isTrue()
        assertThat(
            matchRegexs(
                "https://mega.app/collection/",
                ALBUM_LINK_REGEXS
            )
        ).isFalse()

        assertThat(
            matchRegexs(
                "",
                ALBUM_LINK_REGEXS
            )
        ).isFalse()
    }
}