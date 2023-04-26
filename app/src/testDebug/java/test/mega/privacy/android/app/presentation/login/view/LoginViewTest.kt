package test.mega.privacy.android.app.presentation.login.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class LoginViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithLoginRequired = LoginState(isLoginRequired = true)

    private fun setupRule(state: LoginState = LoginState()) {
        composeRule.setContent {
            LoginView(
                state = state,
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClicked = {},
                onForgotPassword = {},
                onCreateAccount = {})
        }
    }

    @Test
    fun `test that login title is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.login_to_mega)).assertExists()
    }

    @Test
    fun `test that email field is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.email_text)).assertExists()
    }

    @Test
    fun `test that email error is shown if login is required and the error exists`() {
        setupRule(LoginState(isLoginRequired = true, emailError = LoginError.EmptyEmail))
        composeRule.onNodeWithText(fromId(R.string.error_enter_email)).assertExists()
    }

    @Test
    fun `test that password field is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.password_text)).assertExists()
    }

    @Test
    fun `test that password error is shown if login is required and the error exists`() {
        setupRule(LoginState(isLoginRequired = true, passwordError = LoginError.EmptyPassword))
        composeRule.onNodeWithText(fromId(R.string.error_enter_password)).assertExists()
    }

    @Test
    fun `test that log in button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.login_text)).assertExists()
    }

    @Test
    fun `test forgot your password button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.forgot_pass)).assertExists()
    }

    @Test
    fun `test that new to mega text is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.new_to_mega)).assertExists()
    }

    @Test
    fun `test that create account button is shown if login is required`() {
        setupRule(stateWithLoginRequired)
        composeRule.onNodeWithText(fromId(R.string.create_account)).assertExists()
    }
}