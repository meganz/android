package mega.privacy.android.app.presentation.login.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.MultiFactorAuthState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TwoFactorAuthenticationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun test_that_content_display_correctly() {
        createCompose(
            state = LoginState(
                isLoginNewDesignEnabled = true,
            )
        )

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.lost_your_authenticator_device))
            .assertExists()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.multi_factor_auth_login_verification_content))
            .assertExists()
        composeTestRule.onNodeWithTag(TWO_FA_INPUT_FIELD_TEST_TAG)
            .assertExists()
        composeTestRule.onNodeWithTag(TWO_FA_PROGRESS_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun test_that_error_show_correctly() {
        createCompose(
            state = LoginState(
                isLoginNewDesignEnabled = true,
                multiFactorAuthState = MultiFactorAuthState.Failed
            )
        )

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.multi_factor_auth_login_verification_input_error_text))
            .assertExists()
    }

    @Test
    fun test_that_loading_indicator_shows_correctly() {
        createCompose(
            state = LoginState(
                isLoginNewDesignEnabled = true,
                multiFactorAuthState = MultiFactorAuthState.Checking
            )
        )

        composeTestRule.onNodeWithTag(TWO_FA_PROGRESS_TEST_TAG)
            .assertExists()
    }

    private fun createCompose(
        state: LoginState,
        on2FAChanged: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            NewTwoFactorAuthentication(
                state = state,
                on2FAChanged = on2FAChanged,
                onLostAuthenticatorDevice = {},
            )
        }
    }
}