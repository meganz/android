package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.view.TWO_FA_PROGRESS_TEST_TAG
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AUTHENTICATION_SCREEN_PIN_FIELD_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class AuthenticationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setupRule(uiState: TwoFactorAuthenticationUIState = TwoFactorAuthenticationUIState()) {
        composeRule.setContent {
            AuthenticationScreen(
                uiState = uiState,
                on2FAChanged = {},
            )
        }
    }

    @Test
    fun `test that authentication instruction message is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(sharedR.string.multi_factor_auth_login_verification_content))
            .assertIsDisplayed()
    }

    @Test
    fun `test that error is shown when authenticationState is equal to Failed`() {
        setupRule(TwoFactorAuthenticationUIState(authenticationState = AuthenticationState.Failed))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertIsDisplayed()
    }

    @Test
    fun `test that error is not shown when authenticationState is equal to Checking`() {
        setupRule(TwoFactorAuthenticationUIState(authenticationState = AuthenticationState.Checking))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertDoesNotExist()
    }

    @Test
    fun `test that error is not shown when authenticationState is equal to Fixed`() {
        setupRule(TwoFactorAuthenticationUIState(authenticationState = AuthenticationState.Fixed))
        composeRule.onNodeWithText(fromId(R.string.pin_error_2fa)).assertDoesNotExist()
    }

    @Test
    fun `test that 2FA input field is shown`() {
        setupRule()
        composeRule.onNodeWithTag(AUTHENTICATION_SCREEN_PIN_FIELD_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that progressbar is shown when authenticationState is equal to Checking`() {
        setupRule(TwoFactorAuthenticationUIState(authenticationState = AuthenticationState.Checking))
        composeRule.onNodeWithTag(TWO_FA_PROGRESS_TEST_TAG).assertIsDisplayed()
    }
}