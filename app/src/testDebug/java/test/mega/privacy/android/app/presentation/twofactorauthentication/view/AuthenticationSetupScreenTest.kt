package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import android.graphics.Bitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationSetupScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.CONTENT_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.INSTRUCTIONS_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.QR_CODE_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.SEED_BOX_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.SETUP_PROGRESSBAR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class AuthenticationSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setupRule(uiState: TwoFactorAuthenticationUIState = TwoFactorAuthenticationUIState()) {
        composeRule.setContent {
            AuthenticationSetupScreen(
                uiState = uiState,
                isDarkMode = false,
                qrCodeMapper = { _, _, _, _, _ ->
                    Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
                },
                isIntentAvailable = { true },
                onNextClicked = {},
                onOpenInClicked = {},
                openPlayStore = {},
                onCopySeedLongClicked = {}
            )
        }
    }

    @Test
    fun `test that loading progressbar is shown when is2FAFetchCompleted is false`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = false))
        composeRule.onNodeWithTag(SETUP_PROGRESSBAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that Content view is not shown when is2FAFetchCompleted is false`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = false))
        composeRule.onNodeWithTag(CONTENT_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that Content view is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithTag(CONTENT_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that InstructionsBox is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithTag(INSTRUCTIONS_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that SeedsBox is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithTag(SEED_BOX_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that QRCode is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithTag(QR_CODE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that Next button is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithText(fromId(R.string.general_next)).assertIsDisplayed()
    }

    @Test
    fun `test that Next button is clickable when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithText(fromId(R.string.general_next)).assertHasClickAction()
    }

    @Test
    fun `test that OpenIn button is shown when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithText(fromId(R.string.open_app_button)).assertIsDisplayed()
    }

    @Test
    fun `test that OpenIn button is clickable when is2FAFetchCompleted is true`() {
        setupRule(TwoFactorAuthenticationUIState(is2FAFetchCompleted = true))
        composeRule.onNodeWithText(fromId(R.string.open_app_button)).assertHasClickAction()
    }
}