package test.mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.view.LoginTestTags
import mega.privacy.android.app.presentation.login.view.NewLoginView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewLoginViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithLoginRequired = LoginState(isLoginRequired = true)

    private fun setupRule(
        state: LoginState = LoginState(),
        onLoginClicked: () -> Unit = { },
        onForgotPassword: () -> Unit = { },
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
}