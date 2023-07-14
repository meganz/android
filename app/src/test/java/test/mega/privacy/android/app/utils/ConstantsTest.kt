package test.mega.privacy.android.app.utils

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.Constants.ACCOUNT_INVITATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.BUSINESS_INVITE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CONFIRMATION_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.EMAIL_VERIFY_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.FILE_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.NEW_MESSAGE_CHAT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.PENDING_CONTACTS_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.RESET_PASSWORD_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS
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
        assertThat(matchRegexs("https://mega.co.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#!def", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/file/abc", FILE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/file/abc#!def", FILE_LINK_REGEXS)).isTrue()
    }

    /**
     * test Regex of Constants.CONFIRMATION_LINK_REGEXS
     */
    @Test
    fun test_Constants_CONFIRMATION_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#confirm.+$"
        assertThat(matchRegexs("https://mega.co.nz/abc#confirmxxxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#confirmdxxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#confirm", CONFIRMATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*confirm.+$"
        assertThat(matchRegexs("https://mega.co.nz/confirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxconfirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#confirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/confirmxxxx", CONFIRMATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#confirm.+$"
        assertThat(matchRegexs("https://mega.nz/abc#confirmxxxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#confirmdxxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#confirm", CONFIRMATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*confirm.+$"
        assertThat(matchRegexs("https://mega.nz/confirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxconfirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#confirmxxxx", CONFIRMATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/confirm", CONFIRMATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/confirmxxxx", CONFIRMATION_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", CONFIRMATION_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.ACCOUNT_INVITATION_LINK_REGEXS
     */
    @Test
    fun test_Constants_ACCOUNT_INVITATION_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#newsignup.+$"
        assertThat(matchRegexs("https://mega.co.nz/abc#newsignupxxxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#newsignupdxxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*newsignup.+$"
        assertThat(matchRegexs("https://mega.co.nz/newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxnewsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#newsignup.+$"
        assertThat(matchRegexs("https://mega.nz/abc#newsignupxxxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#newsignupdxxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*newsignup.+$"
        assertThat(matchRegexs("https://mega.nz/newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxnewsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/newsignup", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/newsignupxxxx", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", ACCOUNT_INVITATION_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.NEW_MESSAGE_CHAT_LINK_REGEXS
     */
    @Test
    fun test_Constants_NEW_MESSAGE_CHAT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#fm/chat"
        assertThat(matchRegexs("https://mega.co.nz/#fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxx#fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#fm/chatxxx", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/#fm/CHATxxx", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*fm/chat"
        assertThat(matchRegexs("https://mega.co.nz/fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxfm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/fm/chatXXX", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxfm/chatXXX", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nzxx/fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#fm/chat"
        assertThat(matchRegexs("https://mega.nz/#fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxx#fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#fm/chatxxx", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.cnz/#fm/CHATxxx", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*fm/chat"
        assertThat(matchRegexs("https://mega.nz/fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxfm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/fm/chatXXX", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxfm/chatXXX", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nzxx/fm/chat", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", NEW_MESSAGE_CHAT_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.CANCEL_ACCOUNT_LINK_REGEXS
     */
    @Test
    fun test_Constants_CANCEL_ACCOUNT_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#cancel.+$"
        assertThat(matchRegexs("https://mega.co.nz/abc#cancelxxxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#canceldxxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()


        // test "^https://mega\\.co\\.nz/.*cancel.+$"
        assertThat(matchRegexs("https://mega.co.nz/cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxcancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#cancel.+$"
        assertThat(matchRegexs("https://mega.nz/abc#cancelxxxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#canceldxxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*cancel.+$"
        assertThat(matchRegexs("https://mega.nz/cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#cancelQIsyQY4BAc8QBCyAwUt03YqB", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxcancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/cancel", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/cancelxxxx", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", CANCEL_ACCOUNT_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS
     */
    @Test
    fun test_Constants_VERIFY_CHANGE_MAIL_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#verify.+$"
        assertThat(matchRegexs("https://mega.co.nz/abc#verifyxxxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#verifydxxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*verify.+$"
        assertThat(matchRegexs("https://mega.co.nz/verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxverifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#verify.+$"
        assertThat(matchRegexs("https://mega.nz/abc#verifyxxxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#verifydxxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*verify.+$"
        assertThat(matchRegexs("https://mega.nz/verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxverifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/verify", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/verifyxxxx", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", VERIFY_CHANGE_MAIL_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.RESET_PASSWORD_LINK_REGEXS
     */
    @Test
    fun test_Constants_RESET_PASSWORD_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#recover.+$"
        assertThat(matchRegexs("https://mega.co.nz/abc#recoverxxxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#recoverdxxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/abc#recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*recover.+$"
        assertThat(matchRegexs("https://mega.co.nz/recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxrecoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#recover.+$"
        assertThat(matchRegexs("https://mega.nz/abc#recoverxxxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#recoverdxxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/abc#recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*recover.+$"
        assertThat(matchRegexs("https://mega.nz/recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxrecoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/recover", RESET_PASSWORD_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/recoverxxxx", RESET_PASSWORD_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", RESET_PASSWORD_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.PENDING_CONTACTS_LINK_REGEXS
     */
    @Test
    fun test_Constants_PENDING_CONTACTS_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/.*#fm/ipc"
        assertThat(matchRegexs("https://mega.co.nz/#fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxx#fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#fm/ipcxxx", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/#fm/IPCxxx", PENDING_CONTACTS_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/.*fm/ipc"
        assertThat(matchRegexs("https://mega.co.nz/fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/xxxfm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/fm/ipcXXX", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxfm/ipcXXX", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nzxx/fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nzxx/fm/IPC", PENDING_CONTACTS_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*#fm/ipc"
        assertThat(matchRegexs("https://mega.nz/#fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxx#fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#fm/ipcxxx", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.cnz/#fm/CHATxxx", PENDING_CONTACTS_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/.*fm/ipc"
        assertThat(matchRegexs("https://mega.nz/fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/xxxfm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/fm/ipcXXX", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxfm/ipcXXX", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nzxx/fm/ipc", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nzxx/fm/IPC", PENDING_CONTACTS_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", PENDING_CONTACTS_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.EMAIL_VERIFY_LINK_REGEXS
     */
    @Test
    fun test_Constants_EMAIL_VERIFY_LINK_REGEXS() {
        // test "^https://mega\\.co\\.nz/#emailverify.+$"
        assertThat(matchRegexs("https://mega.co.nz/#emailverifyxxxxx", EMAIL_VERIFY_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/abc#emailverifyxxxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/abc#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/emailverify.+$"
        assertThat(matchRegexs("https://mega.co.nz/emailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxemailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxemailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/emailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/#emailverify.+$"
        assertThat(matchRegexs("https://mega.nz/#emailverifyxxxxx", EMAIL_VERIFY_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/abc#emailverifyxxxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/abc#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/emailverify.+$"
        assertThat(matchRegexs("https://mega.nz/emailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/emailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxemailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxemailverify", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/emailverifyxxxx", EMAIL_VERIFY_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", EMAIL_VERIFY_LINK_REGEXS)).isFalse()
    }

    /**
     * test Regex of Constants.BUSINESS_INVITE_LINK_REGEXS
     */
    @Test
    fun test_Constants_BUSINESS_INVITE_LINK_REGEXS() {

        // test "^https://mega\\.co\\.nz/#businessinvite.+$"
        assertThat(matchRegexs("https://mega.co.nz/#businessinvitexxxxx", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/abc#businessinvitexxxxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/abc#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.co\\.nz/businessinvite.+$"
        assertThat(matchRegexs("https://mega.co.nz/businessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.co.nz/businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxbusinessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.co.nz/xxxbusinessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/businessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/#businessinvite.+$"
        assertThat(matchRegexs("https://mega.nz/#businessinvitexxxxx", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#businessinviteL6NfzzIHAAcQegDg7Uko", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/abc#businessinvitexxxxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/abc#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/#businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()

        // test "^https://mega\\.nz/businessinvite.+$"
        assertThat(matchRegexs("https://mega.nz/businessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/businessinviteL6NfzzIHAAcQegDg7Uko", BUSINESS_INVITE_LINK_REGEXS)).isTrue()
        assertThat(matchRegexs("https://mega.nz/businessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxbusinessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.nz/xxxbusinessinvite", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
        assertThat(matchRegexs("https://mega.io/businessinvitexxxx", BUSINESS_INVITE_LINK_REGEXS)).isFalse()

        assertThat(matchRegexs("", BUSINESS_INVITE_LINK_REGEXS)).isFalse()
    }
}