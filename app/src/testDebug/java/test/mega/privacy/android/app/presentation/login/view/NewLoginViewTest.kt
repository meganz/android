package test.mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginTestTags
import mega.privacy.android.app.presentation.login.view.NewLoginView
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class NewLoginViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithLoginRequired = LoginState(isLoginRequired = true)

    private fun setupRule(
        state: LoginState = LoginState(),
        onLoginClicked: () -> Unit = { },
        onForgotPassword: () -> Unit = { },
        stopLogin: () -> Unit = { }
    ) {
        composeRule.setContent {
            NewLoginView(
                state = state,
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClicked = onLoginClicked,
                onForgotPassword = onForgotPassword,
                onCreateAccount = {},
                onSnackbarMessageConsumed = {},
                on2FAChanged = {},
                onLostAuthenticatorDevice = {},
                onBackPressed = {},
                onReportIssue = {},
                onResetAccountBlockedEvent = {},
                onResendVerificationEmail = {},
                onResetResendVerificationEmailEvent = {},
                stopLogin = stopLogin
            )
        }
    }

    @Test
    fun `test that all text fields are displayed correctly`() {
        setupRule(stateWithLoginRequired)

        composeRule.onNodeWithText(fromId(sharedR.string.login_page_title))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_text))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.password_text))
            .assertExists()
        composeRule.onNodeWithText(
            fromId(sharedR.string.login_page_sign_up_action_footer_text)
                .replace("[A]", "")
                .replace("[/A]", "")
        ).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.LOGIN_BUTTON)
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.SIGN_UP_BUTTON)
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.FORGOT_PASSWORD_BUTTON)
            .assertExists()
    }

    @Test
    fun `test that invalid email address shows when email is invalid`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                emailError = LoginError.NotValidEmail
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that invalid email address shows when email is empty`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                emailError = LoginError.EmptyEmail
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that invalid password shows when password is empty`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                passwordError = LoginError.EmptyPassword
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_password_error_message))
            .assertExists()
        composeRule.onNodeWithText(fromId(sharedR.string.login_invalid_email_error_message))
            .assertDoesNotExist()
    }

    @Test
    fun `test that wrong email or password shows when email or password is incorrect`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                loginException = LoginWrongEmailOrPassword()
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_wrong_credential_error_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.WRONG_CREDENTIAL_BANNER)
            .assertExists()
    }

    @Test
    fun `test that too many attempts shows when too many attempts`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                loginException = LoginTooManyAttempts()
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_too_many_attempts_error_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.TOO_MANY_ATTEMPTS_BANNER)
            .assertExists()
    }

    @Test
    fun `test that account blocked dialog is shown when tos copyright account blocked event is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.TOS_COPYRIGHT,
                        text = fromId(sharedR.string.dialog_account_suspended_ToS_copyright_message)
                    )
                )
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.dialog_account_suspended_ToS_copyright_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG)
            .assertExists()
    }

    @Test
    fun `test that account blocked dialog is shown when tos non copyright account blocked event is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.TOS_NON_COPYRIGHT,
                        text = fromId(sharedR.string.dialog_account_suspended_ToS_non_copyright_message)
                    )
                )
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.dialog_account_suspended_ToS_non_copyright_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG)
            .assertExists()
    }


    @Test
    fun `test that account blocked dialog is shown when subuser disabled account blocked event is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.SUBUSER_DISABLED,
                        text = fromId(sharedR.string.error_business_disabled)
                    )
                )
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.error_business_disabled))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG)
            .assertExists()
    }


    @Test
    fun `test that account locked dialog is shown when account blocked event with email verification is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.VERIFICATION_EMAIL,
                        text = fromId(sharedR.string.login_account_suspension_email_verification_message)
                    )
                )
            )
        )

        composeRule.onNodeWithText(fromId(sharedR.string.login_account_suspension_email_verification_message))
            .assertExists()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_LOCKED_DIALOG)
            .assertExists()
    }

    @Test
    fun `test that account blocked or locked dialog is not shown when account blocked event with sms verification is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.VERIFICATION_SMS,
                        text = "sms verification"
                    )
                )
            )
        )

        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_LOCKED_DIALOG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that account blocked or locked dialog is not shown when account blocked event with sub user removed is detected`() {
        setupRule(
            state = stateWithLoginRequired.copy(
                accountBlockedEvent = triggered(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.SUBUSER_REMOVED,
                        text = "business user removed"
                    )
                )
            )
        )

        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_LOCKED_DIALOG)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(LoginTestTags.ACCOUNT_BLOCKED_DIALOG)
            .assertDoesNotExist()
    }


    @Test
    fun `test that success from resendVerificationEmailEvent is handled`() {
        val stopLogin: () -> Unit = mock()
        setupRule(
            state = stateWithLoginRequired.copy(
                resendVerificationEmailEvent = triggered(true)
            ), stopLogin = stopLogin
        )

        composeRule.onNodeWithText(fromId(sharedR.string.general_email_resend_success_message))
            .assertExists()
        verify(stopLogin).invoke()
    }


    @Test
    fun `test that failure from resendVerificationEmailEvent is handled`() {
        val stopLogin: () -> Unit = mock()
        setupRule(
            state = stateWithLoginRequired.copy(
                resendVerificationEmailEvent = triggered(false)
            ), stopLogin = stopLogin
        )

        composeRule.onNodeWithText(fromId(sharedR.string.general_request_failed_message))
            .assertExists()
        verify(stopLogin).invoke()
    }
}