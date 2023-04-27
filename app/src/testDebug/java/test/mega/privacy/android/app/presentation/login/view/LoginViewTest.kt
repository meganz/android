package test.mega.privacy.android.app.presentation.login.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.FETCH_NODES_PROGRESS_TEST_TAG
import mega.privacy.android.app.presentation.login.view.LOGIN_PROGRESS_TEST_TAG
import mega.privacy.android.app.presentation.login.view.LoginView
import mega.privacy.android.app.presentation.login.view.MEGA_LOGO_TEST_TAG
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesTemporaryError
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
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
                onCreateAccount = {},
                onSnackbarMessageConsumed = {}
            )
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

    @Test
    fun `test that MEGA logo is shown if login is in progress`() {
        setupRule(LoginState(isLoginInProgress = true))
        composeRule.onNodeWithTag(MEGA_LOGO_TEST_TAG).assertExists()
    }

    @Test
    fun `test that checking signup link text is shown if login is in progress and it is processing a link`() {
        setupRule(LoginState(isLoginInProgress = true, isCheckingSignupLink = true))
        composeRule.onNodeWithText(fromId(R.string.login_querying_signup_link)).assertExists()
    }

    @Test
    fun `test that connecting to server text is shown if login is in progress`() {
        setupRule(LoginState(isLoginInProgress = true))
        composeRule.onNodeWithText(fromId(R.string.login_connecting_to_server)).assertExists()
    }

    @Test
    fun `test that login progress bar is shown if login is in progress`() {
        setupRule(LoginState(isLoginInProgress = true))
        composeRule.onNodeWithTag(LOGIN_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test that updating file list text is shown if fetch nodes update exists`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate()))
        composeRule.onNodeWithText(fromId(R.string.download_updating_filelist)).assertExists()
    }

    @Test
    fun `test that login progress bar is shown if login fetch nodes exists`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate()))
        composeRule.onNodeWithTag(LOGIN_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test that preparing file list text is shown if the progress of fetch nodes is greater than 0`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate(Progress(0.5F))))
        composeRule.onNodeWithText(fromId(R.string.login_preparing_filelist)).assertExists()
    }

    @Test
    fun `test that fetch nodes progress bar is shown if the progress of fetch nodes is greater than 0`() {
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate(Progress(0.5F))))
        composeRule.onNodeWithTag(FETCH_NODES_PROGRESS_TEST_TAG).assertExists()
    }

    @Test
    fun `test temporary error text is shown if fetch nodes update has it`() {
        val temporaryError = FetchNodesTemporaryError.ConnectivityIssues
        setupRule(LoginState(fetchNodesUpdate = FetchNodesUpdate(temporaryError = temporaryError)))
        composeRule.onNodeWithText(fromId(temporaryError.messageId)).assertExists()
    }
}