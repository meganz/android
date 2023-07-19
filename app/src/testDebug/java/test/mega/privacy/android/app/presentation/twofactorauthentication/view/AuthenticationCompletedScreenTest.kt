package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.AuthenticationCompletedScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.RK_EXPORT_BOX_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.RK_EXPORT_INSTRUCTION_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class AuthenticationCompletedScreenTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val stateWithMasterKeyExported =
        TwoFactorAuthenticationUIState(isMasterKeyExported = true)


    private fun setupRule(uiState: TwoFactorAuthenticationUIState = TwoFactorAuthenticationUIState()) {
        composeRule.setContent {
            AuthenticationCompletedScreen(
                isMasterKeyExported = uiState.isMasterKeyExported,
                onExportRkClicked = {},
                onDismissClicked = {}
            )
        }
    }

    @Test
    fun `test that recovery key instruction is shown`() {
        setupRule()
        composeRule.onNodeWithTag(RK_EXPORT_INSTRUCTION_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that 2FA title is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.title_2fa_enabled)).assertIsDisplayed()
    }

    @Test
    fun `test that 2FA description message is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.description_2fa_enabled)).assertIsDisplayed()
    }

    @Test
    fun `test that recovery key export box is shown`() {
        setupRule()
        composeRule.onNodeWithTag(RK_EXPORT_BOX_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that Export key button is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.general_export)).assertIsDisplayed()
    }

    @Test
    fun `test that Dismiss key button is shown when isMasterKeyExported is equal to true`() {
        setupRule(stateWithMasterKeyExported)
        composeRule.onNodeWithText(fromId(R.string.general_dismiss)).assertIsDisplayed()
    }

    @Test
    fun `test that Dismiss key button is not visible when isMasterKeyExported is equal to false`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.general_dismiss)).assertDoesNotExist()
    }

    @Test
    fun `test that Dismiss key button is clickable`() {
        setupRule(stateWithMasterKeyExported)
        composeRule.onNodeWithText(fromId(R.string.general_dismiss)).assertHasClickAction()
    }

    @Test
    fun `test that Export key button is clickable`() {
        setupRule(TwoFactorAuthenticationUIState())
        composeRule.onNodeWithText(fromId(R.string.general_export)).assertHasClickAction()
    }
}