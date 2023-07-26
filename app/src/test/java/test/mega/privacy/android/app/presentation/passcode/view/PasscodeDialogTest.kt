package test.mega.privacy.android.app.presentation.passcode.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.app.presentation.passcode.view.FAILED_ATTEMPTS_TAG
import mega.privacy.android.app.presentation.passcode.view.FORGOT_PASSCODE_BUTTON_TAG
import mega.privacy.android.app.presentation.passcode.view.LOGOUT_BUTTON_TAG
import mega.privacy.android.app.presentation.passcode.view.PASSCODE_FIELD_TAG
import mega.privacy.android.app.presentation.passcode.view.PASSWORD_FIELD_TAG
import mega.privacy.android.app.presentation.passcode.view.PasscodeDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
internal class PasscodeDialogTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val passcodeUnlockViewModel: PasscodeUnlockViewModel = mock()

    @Test
    fun `test that passcode field is shown`() {
        val uiState = PasscodeUnlockState(
            failedAttempts = 0,
            logoutWarning = false
        )

        displayDialogWithState(uiState)

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that attempts are displayed if above 0`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FAILED_ATTEMPTS_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that attempts are not displayed if 0`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 0,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FAILED_ATTEMPTS_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that logout button is displayed if attempts are greater than 0`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(LOGOUT_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that forgot passcode button is displayed if attempts are greater than 0`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that password field is displayed instead of passcode field when forgot password is tapped`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .performClick()

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(PASSWORD_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that logout and forgot passcode is not displayed when password field is displayed`() {
        displayDialogWithState(
            PasscodeUnlockState(
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .performClick()

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(LOGOUT_BUTTON_TAG)
            .assertDoesNotExist()
    }

    private fun displayDialogWithState(uiState: PasscodeUnlockState) {
        passcodeUnlockViewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    uiState
                )
            )
        }

        composeTestRule.setContent {
            PasscodeDialog(passcodeUnlockViewModel)
        }
    }
}