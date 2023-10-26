package test.mega.privacy.android.app.presentation.passcode.view

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.passcode.PasscodeUnlockViewModel
import mega.privacy.android.app.presentation.passcode.model.PasscodeUIType
import mega.privacy.android.app.presentation.passcode.model.PasscodeUnlockState
import mega.privacy.android.app.presentation.passcode.view.FAILED_ATTEMPTS_TAG
import mega.privacy.android.app.presentation.passcode.view.FORGOT_PASSCODE_BUTTON_TAG
import mega.privacy.android.app.presentation.passcode.view.LOGOUT_BUTTON_TAG
import mega.privacy.android.app.presentation.passcode.view.PASSCODE_FIELD_TAG
import mega.privacy.android.app.presentation.passcode.view.PASSWORD_FIELD_TAG
import mega.privacy.android.app.presentation.passcode.view.PasscodeView
import test.mega.privacy.android.app.AnalyticsTestRule
import mega.privacy.mobile.analytics.event.ForgotPasscodeButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeBiometricUnlockDialogEvent
import mega.privacy.mobile.analytics.event.PasscodeEnteredEvent
import mega.privacy.mobile.analytics.event.PasscodeLogoutButtonPressedEvent
import mega.privacy.mobile.analytics.event.PasscodeUnlockDialogEvent
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
internal class PasscodeViewTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private val passcodeUnlockViewModel: PasscodeUnlockViewModel = mock()
    private val biometricAuthIsAvailable = mock<(Context) -> Boolean>()

    private val showBiometricAuth =
        mock<(onSuccess: () -> Unit, onError: () -> Unit, onFail: () -> Unit, context: Context, promptInfo: BiometricPrompt.PromptInfo, cryptoObject: CryptoObject) -> Unit>()

    @Before
    internal fun setUp() {
        Mockito.clearInvocations(
            passcodeUnlockViewModel,
        )
    }

    @Test
    fun `test that passcode field is shown`() {
        val uiState = PasscodeUnlockState.Data(
            passcodeType = PasscodeUIType.Pin(false, 4),
            failedAttempts = 0,
            logoutWarning = false
        )

        displayPasscodeViewWithState(uiState)

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that attempts are displayed if above 0`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FAILED_ATTEMPTS_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that attempts are not displayed if 0`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 0,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FAILED_ATTEMPTS_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that logout button is displayed if attempts are greater than 0`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(LOGOUT_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that forgot passcode button is displayed if attempts are greater than 0`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that password field is displayed instead of passcode field when forgot password is tapped`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
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
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
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

    @Test
    fun `test that verify with password is called if password is entered for forgotten passcode`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .performClick()

        val expected = "Expected"

        val passwordField =
            composeTestRule.onNode(
                hasAnyAncestor(hasTestTag(PASSWORD_FIELD_TAG)) and hasImeAction(
                    ImeAction.Done
                )
            )

        passwordField.performTextInput(expected)
        passwordField.performImeAction()

        verify(passcodeUnlockViewModel).unlockWithPassword(expected)

    }

    @Test
    fun `test that alphanumeric passcode type displays password field`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Alphanumeric(false),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that verify with passcode is called if alphanumeric passcode is entered`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Alphanumeric(false),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        val expected = "Expected"

        val passwordField =
            composeTestRule.onNode(
                hasAnyAncestor(hasTestTag(PASSWORD_FIELD_TAG)) and hasImeAction(
                    ImeAction.Done
                )
            )

        passwordField.performTextInput(expected)
        passwordField.performImeAction()

        verify(passcodeUnlockViewModel).unlockWithPasscode(expected)

    }

    @Test
    fun `test that biometric auth is launched if available and enabled`() {
        biometricAuthIsAvailable.stub {
            on { invoke(any()) }.thenReturn(true)
        }

        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Alphanumeric(true),
                failedAttempts = 0,
                logoutWarning = false
            )
        )

        verify(showBiometricAuth).invoke(any(), any(), any(), any(), any(), any())

    }

    @Test
    fun `test that biometric error falls back to passcode`() {
        biometricAuthIsAvailable.stub {
            on { invoke(any()) }.thenReturn(true)
        }

        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(true, 4),
                failedAttempts = 0,
                logoutWarning = false
            )
        )

        val onError = argumentCaptor<() -> Unit>()

        verify(showBiometricAuth).invoke(any(), onError.capture(), any(), any(), any(), any())

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertDoesNotExist()

        onError.firstValue.invoke()

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }


    //Analytics
    @Test
    fun `test that passcode dialog event is emitted when displayed`() {
        val uiState = PasscodeUnlockState.Data(
            passcodeType = PasscodeUIType.Pin(false, 4),
            failedAttempts = 0,
            logoutWarning = false
        )

        displayPasscodeViewWithState(uiState)

        composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG, useUnmergedTree = true)
            .assertIsDisplayed()

        assertThat(analyticsRule.events).contains(PasscodeUnlockDialogEvent)
    }

    @Test
    fun `test that biometric dialog event is emitted when biometric dialog is displayed`() {
        biometricAuthIsAvailable.stub {
            on { invoke(any()) }.thenReturn(true)
        }

        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Alphanumeric(true),
                failedAttempts = 0,
                logoutWarning = false
            )
        )

        verify(showBiometricAuth).invoke(any(), any(), any(), any(), any(), any())

        assertThat(analyticsRule.events).contains(PasscodeBiometricUnlockDialogEvent)
    }

    @Test
    @Ignore
    fun `test that passcode entered event is emitted when the pin type passcode is entered`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        val expected = "1234"

        val passwordField =
            composeTestRule.onNodeWithTag(PASSCODE_FIELD_TAG)

        passwordField.performTextInput(expected)

        verify(passcodeUnlockViewModel).unlockWithPasscode(expected)
        assertThat(analyticsRule.events).contains(PasscodeEnteredEvent)
    }

    @Test
    fun `test that passcode entered event is emitted when the password type passcode is entered`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Alphanumeric(false),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        val expected = "Expected"

        val passwordField =
            composeTestRule.onNode(
                hasAnyAncestor(hasTestTag(PASSWORD_FIELD_TAG)) and hasImeAction(
                    ImeAction.Done
                ), useUnmergedTree = false
            )

        passwordField.performTextInput(expected)
        passwordField.performImeAction()

        verify(passcodeUnlockViewModel).unlockWithPasscode(expected)
        assertThat(analyticsRule.events).contains(PasscodeEnteredEvent)
    }

    @Test
    fun `test that forgot passcode event is fired when forgot passcode is clicked`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(FORGOT_PASSCODE_BUTTON_TAG)
            .performClick()

        assertThat(analyticsRule.events).contains(ForgotPasscodeButtonPressedEvent)
    }

    @Test
    fun `test that logout button pressed event is fired`() {
        displayPasscodeViewWithState(
            PasscodeUnlockState.Data(
                passcodeType = PasscodeUIType.Pin(false, 4),
                failedAttempts = 1,
                logoutWarning = false
            )
        )

        composeTestRule.onNodeWithTag(LOGOUT_BUTTON_TAG).performClick()

        assertThat(analyticsRule.events).contains(PasscodeLogoutButtonPressedEvent)
    }

    private fun displayPasscodeViewWithState(uiState: PasscodeUnlockState) {
        passcodeUnlockViewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    uiState
                )
            )
        }

        val cryptoObject = mock<CryptoObject>()
        composeTestRule.setContent {
            PasscodeView(
                passcodeUnlockViewModel = passcodeUnlockViewModel,
                biometricAuthIsAvailable = biometricAuthIsAvailable,
                showBiometricAuth = showBiometricAuth,
                cryptObjectFactory = mock {
                    on { invoke() }.thenReturn(
                        cryptoObject
                    )
                }
            )
        }
    }
}