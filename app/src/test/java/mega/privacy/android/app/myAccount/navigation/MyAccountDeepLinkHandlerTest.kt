package mega.privacy.android.app.myAccount.navigation


import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.resetpassword.ResetPasswordLinkInfo
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.usecase.QueryResetPasswordLinkUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyAccountDeepLinkHandlerTest {

    private lateinit var underTest: MyAccountDeepLinkHandler

    private val queryResetPasswordLinkUseCase = mock<QueryResetPasswordLinkUseCase>()
    private val getAccountCredentialsUseCase = mock<GetAccountCredentialsUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    @BeforeAll
    fun setup() {
        underTest = MyAccountDeepLinkHandler(
            queryResetPasswordLinkUseCase = queryResetPasswordLinkUseCase,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            snackbarEventQueue = snackbarEventQueue,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            queryResetPasswordLinkUseCase,
            getAccountCredentialsUseCase,
            snackbarEventQueue,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches ACTION_CANCEL_ACCOUNT pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://cancelAccount"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.CANCEL_ACCOUNT_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                MyAccountNavKey(
                    action = Constants.ACTION_CANCEL_ACCOUNT,
                    link = uriString
                )
            )
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches VERIFY_CHANGE_MAIL_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://verifyEmail"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.VERIFY_CHANGE_MAIL_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                MyAccountNavKey(
                    action = Constants.ACTION_CHANGE_MAIL,
                    link = uriString
                )
            )
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @Test
    fun `test that empty list is returned when uri matches RESET_PASSWORD_LINK pattern type, app is logged in and link for email is not the same account email`() =
        runTest {
            val uriString = "mega://resetPassword"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }
            val result = mock<ResetPasswordLinkInfo> {
                on { this.email } doReturn "link@mega.nz"
                on { isRequiredRecoveryKey } doReturn false
            }
            val accountCredentials = mock<UserCredentials> {
                on { this.email } doReturn "account@mega.nz"
            }

            whenever(queryResetPasswordLinkUseCase(uriString)) doReturn result
            whenever(getAccountCredentialsUseCase()) doReturn accountCredentials

            val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, true)

            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(R.string.error_not_logged_with_correct_account)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned when uri matches RESET_PASSWORD_LINK pattern type and recovery key is required`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }
        val email = "email@mega.io"
        val result = mock<ResetPasswordLinkInfo> {
            on { this.email } doReturn email
            on { isRequiredRecoveryKey } doReturn true
        }
        val accountCredentials = if (isLoggedIn) {
            mock<UserCredentials> {
                on { this.email } doReturn email
            }
        } else {
            null
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doReturn result
        whenever(getAccountCredentialsUseCase()) doReturn accountCredentials

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                MyAccountNavKey(
                    action = Constants.ACTION_RESET_PASS,
                    link = uriString
                )
            )
        } else {
            assertThat(actual).containsExactly(
                LoginNavKey(
                    action = Constants.ACTION_RESET_PASS,
                    link = uriString
                )
            )
        }

        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that WebSiteNavKey is returned when uri matches RESET_PASSWORD_LINK pattern type and recovery key is not required`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }
        val email = "email@mega.io"
        val result = mock<ResetPasswordLinkInfo> {
            on { this.email } doReturn email
            on { isRequiredRecoveryKey } doReturn false
        }
        val accountCredentials = if (isLoggedIn) {
            mock<UserCredentials> {
                on { this.email } doReturn email
            }
        } else {
            null
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doReturn result
        whenever(getAccountCredentialsUseCase()) doReturn accountCredentials

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        assertThat(actual).containsExactly(WebSiteNavKey(uriString))
        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that empty list is returned when uri matches RESET_PASSWORD_LINK pattern type and query link throws LinkInvalid exception`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doAnswer {
            throw ResetPasswordLinkException.LinkInvalid
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        assertThat(actual).isEmpty()
        verify(snackbarEventQueue).queueMessage(R.string.invalid_link)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that empty list is returned when uri matches RESET_PASSWORD_LINK pattern type and query link throws LinkExpired exception`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doAnswer {
            throw ResetPasswordLinkException.LinkExpired
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        assertThat(actual).isEmpty()
        verify(snackbarEventQueue).queueMessage(R.string.recovery_link_expired)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that empty list is returned when uri matches RESET_PASSWORD_LINK pattern type and query link throws LinkAccessDenied exception`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doAnswer {
            throw ResetPasswordLinkException.LinkAccessDenied
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        assertThat(actual).isEmpty()
        verify(snackbarEventQueue).queueMessage(R.string.error_not_logged_with_correct_account)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that empty list is returned when uri matches RESET_PASSWORD_LINK pattern type and query link throws non ResetPasswordLinkException`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://resetPassword"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        whenever(queryResetPasswordLinkUseCase(uriString)) doThrow RuntimeException()

        val actual = underTest.getNavKeys(uri, RegexPatternType.RESET_PASSWORD_LINK, isLoggedIn)

        assertThat(actual).isEmpty()
        verify(snackbarEventQueue).queueMessage(R.string.general_text_error)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when uri does not match ACTION_CANCEL_ACCOUNT, VERIFY_CHANGE_MAIL_LINK or RESET_PASSWORD_LINK pattern type`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "mega://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }
}