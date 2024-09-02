package mega.privacy.android.app.presentation.settings.passcode.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.passcode.model.PasscodeSettingsUIState
import mega.privacy.android.app.presentation.settings.passcode.model.TimeoutOption
import mega.privacy.android.app.presentation.settings.passcode.view.tile.CHANGE_PASSCODE_TILE
import mega.privacy.android.app.presentation.settings.passcode.view.tile.ENABLE_PASSCODE_TILE
import mega.privacy.android.app.presentation.settings.passcode.view.tile.FINGERPRINT_ID_TILE
import mega.privacy.android.app.presentation.settings.passcode.view.tile.FINGERPRINT_ID_TILE_SWITCH
import mega.privacy.android.app.presentation.settings.passcode.view.tile.REQUIRE_PASSCODE_TILE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PasscodeSettingsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that toolbar is shown`() {
        initialiseView()

        composeTestRule.onNodeWithTag(PASSCODE_SETTINGS_TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun `test that only enable passcode setting is displayed when disabled`() {
        initialiseView(isEnabled = false)

        composeTestRule.onNodeWithTag(ENABLE_PASSCODE_TILE).assertIsDisplayed()

        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE_SWITCH).assertDoesNotExist()
        composeTestRule.onNodeWithTag(REQUIRE_PASSCODE_TILE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CHANGE_PASSCODE_TILE).assertDoesNotExist()
    }

    @Test
    fun `test that all settings are visible if enabled`() {
        initialiseView(
            isEnabled = true,
            hasBiometricCapability = true,
        )

        composeTestRule.onNodeWithTag(ENABLE_PASSCODE_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(REQUIRE_PASSCODE_TILE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHANGE_PASSCODE_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that biometric toggle is displayed if capability is true`() {
        initialiseView(
            isEnabled = true,
            hasBiometricCapability = true,
        )

        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).assertIsDisplayed()
    }

    @Test
    fun `test that biometric toggle is not displayed if capability is false`() {
        initialiseView(
            isEnabled = true,
            hasBiometricCapability = false,
        )

        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE_SWITCH).assertDoesNotExist()
    }

    @Test
    fun `test that view navigates to enable passcode if not yet enabled on toggle`() {
        val navigateToChangePasscode = mock<() -> Unit>()
        initialiseView(isEnabled = false, navigateToChangePasscode = navigateToChangePasscode)

        composeTestRule.onNodeWithTag(ENABLE_PASSCODE_TILE).performClick()

        verify(navigateToChangePasscode).invoke()
    }

    @Test
    fun `test that disable passcode is called if enabled on toggle`() {
        val disablePasscode = mock<() -> Unit>()
        initialiseView(isEnabled = true, onDisablePasscode = disablePasscode)

        composeTestRule.onNodeWithTag(ENABLE_PASSCODE_TILE).performClick()

        verify(disablePasscode).invoke()
    }

    @Test
    fun `test that view navigates to set up biometrics if not yet enabled on toggle`() {
        val navigateToBiometricSetup = mock<() -> Unit>()
        val composableWrapper: @Composable (() -> Unit, () -> Unit) -> Unit = @Composable { _, _ ->
            navigateToBiometricSetup()
        }

        initialiseView(
            isEnabled = true,
            hasBiometricCapability = true,
            isBiometricsEnabled = false,
            authenticateBiometrics = composableWrapper,
        )

        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).performClick()
        composeTestRule.waitForIdle()

        verify(navigateToBiometricSetup).invoke()
    }

    @Test
    fun `test that disable biometrics is called if enabled on toggle`() {
        val disableBiometrics = mock<() -> Unit>()
        initialiseView(
            isEnabled = true,
            hasBiometricCapability = true,
            isBiometricsEnabled = true,
            onDisableBiometrics = disableBiometrics,
        )

        composeTestRule.onNodeWithTag(FINGERPRINT_ID_TILE).performClick()

        verify(disableBiometrics).invoke()
    }

    private fun initialiseView(
        isEnabled: Boolean = false,
        hasBiometricCapability: Boolean = false,
        isBiometricsEnabled: Boolean = false,
        navigateToChangePasscode: () -> Unit = {},
        onDisablePasscode: () -> Unit = {},
        onDisableBiometrics: () -> Unit = {},
        navigateToSelectTimeout: () -> Unit = {},
        authenticateBiometrics: @Composable (onSuccess: () -> Unit, onComplete: () -> Unit) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            PasscodeSettingsView(
                state = PasscodeSettingsUIState(
                    isEnabled = isEnabled,
                    isBiometricsEnabled = isBiometricsEnabled,
                    timeout = TimeoutOption.MinutesTimeSpan(
                        timeoutInMinutes = 1,
                    )
                ),
                onDisablePasscode = onDisablePasscode,
                onDisableBiometrics = onDisableBiometrics,
                navigateToChangePasscode = navigateToChangePasscode,
                navigateToSelectTimeout = navigateToSelectTimeout,
                hasBiometricCapability = hasBiometricCapability,
                authenticateBiometrics = authenticateBiometrics
            )
        }
    }
}