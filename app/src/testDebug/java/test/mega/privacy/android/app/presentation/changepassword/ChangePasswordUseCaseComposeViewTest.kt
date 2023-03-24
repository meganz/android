package test.mega.privacy.android.app.presentation.changepassword

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.extensions.toStrengthAttribute
import mega.privacy.android.app.presentation.changepassword.model.ChangePasswordUIState
import mega.privacy.android.app.presentation.changepassword.view.ChangePasswordView
import mega.privacy.android.app.presentation.changepassword.view.Constants
import mega.privacy.android.app.presentation.changepassword.view.Constants.CHANGE_PASSWORD_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.ERROR_FOOTER_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.LOADING_DIALOG_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_STRENGTH_BAR_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_STRENGTH_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.SEE_PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.SNACKBAR_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.Constants.TNC_CHECKBOX_TEST_TAG
import mega.privacy.android.app.presentation.changepassword.view.PasswordStrengthBar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.dark_blue_200
import mega.privacy.android.core.ui.theme.dark_blue_500
import mega.privacy.android.core.ui.theme.green_400
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.lime_green_200
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.yellow_300
import mega.privacy.android.core.ui.theme.yellow_600
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.hasBackgroundColor
import test.mega.privacy.android.app.onNodeWithText

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChangePasswordUseCaseComposeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(uiState: ChangePasswordUIState = ChangePasswordUIState()) {
        composeTestRule.setContent {
            ChangePasswordView(
                uiState = uiState,
                onSnackBarShown = {},
                onPasswordTextChanged = {},
                onConfirmPasswordTextChanged = {},
                onTnCLinkClickListener = {},
                onTriggerChangePassword = {},
                onTriggerResetPassword = {},
                onValidatePassword = {},
                onValidateOnSave = { _, _ -> },
                onResetValidationState = {},
                onAfterPasswordChanged = {},
                onAfterPasswordReset = { _, _ -> },
                onPromptedMultiFactorAuth = {},
                onFinishActivity = {},
                onShowAlert = {}
            )
        }
    }

    @Test
    fun `test that on first load should render with correct state`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(PASSWORD_TEST_TAG)
            .assert(hasText(fromId(R.string.my_account_change_password_newPassword1)))
        composeTestRule.onNodeWithTag(Constants.CONFIRM_PASSWORD_TEST_TAG)
            .assert(hasText(fromId(R.string.my_account_change_password_newPassword2)))
        composeTestRule.onNodeWithTag(SEE_PASSWORD_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PASSWORD_STRENGTH_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TNC_CHECKBOX_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CHANGE_PASSWORD_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that toolbar title has correct value when mode is change password`() {
        setComposeContent(ChangePasswordUIState(isResetPasswordMode = false))

        composeTestRule.onAllNodesWithText(fromId(R.string.my_account_change_password))[0].assertIsDisplayed()
    }

    @Test
    fun `test that toolbar title has correct value when mode is reset password`() {
        setComposeContent(ChangePasswordUIState(isResetPasswordMode = true))

        composeTestRule.onNodeWithText(R.string.title_enter_new_password).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar will be shown when snackbar message not null`() {
        setComposeContent(ChangePasswordUIState(snackBarMessage = R.string.general_text_error))

        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasAnyChild(hasText(fromId(R.string.general_text_error))))
    }

    @Test
    fun `test that loading will be shown when loading message not null`() {
        setComposeContent(ChangePasswordUIState(loadingMessage = R.string.general_loading))

        composeTestRule.onNodeWithTag(LOADING_DIALOG_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasAnyChild(hasText(fromId(R.string.general_loading))))
    }

    @Test
    fun `test that when password is current password should change password text field hint`() {
        setComposeContent(ChangePasswordUIState(isCurrentPassword = true))

        composeTestRule.onNodeWithTag(PASSWORD_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.error_same_password)))
    }

    @Test
    fun `test that when an error found but not current password should show error footer text`() {
        setComposeContent(
            ChangePasswordUIState(
                isCurrentPassword = false,
                passwordError = R.string.general_text_error
            )
        )

        composeTestRule.onNodeWithTag(ERROR_FOOTER_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.general_text_error)))
    }

    @Test
    fun `test that when confirm password has an error found but not current password should show error footer text`() {
        setComposeContent(
            ChangePasswordUIState(
                isCurrentPassword = false,
                confirmPasswordError = R.string.general_text_error
            )
        )

        composeTestRule.onNodeWithTag(ERROR_FOOTER_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.general_text_error)))
    }

    @Test
    fun `test that when an error found but current password should not show error footer text but at hint`() {
        setComposeContent(
            ChangePasswordUIState(
                isCurrentPassword = true,
                passwordError = R.string.general_text_error
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.error_same_password)))
        composeTestRule.onNodeWithTag(ERROR_FOOTER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that when text field is on focus should show see password icon`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(PASSWORD_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(SEE_PASSWORD_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that when password strength is not invisible and password error is null should show password strength bar`() {
        setComposeContent(
            ChangePasswordUIState(
                passwordStrength = PasswordStrength.MEDIUM,
                passwordError = null
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_STRENGTH_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that when password strength is not invisible but password has error is null should not show password strength bar`() {
        setComposeContent(
            ChangePasswordUIState(
                passwordStrength = PasswordStrength.MEDIUM,
                passwordError = R.string.general_text_error
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_STRENGTH_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that when password strength is not invisible and has error but is current password should show password strength bar`() {
        setComposeContent(
            ChangePasswordUIState(
                passwordStrength = PasswordStrength.MEDIUM,
                passwordError = R.string.error_same_password,
                isCurrentPassword = true
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_STRENGTH_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that when state is current password should disable change password button`() {
        setComposeContent(
            ChangePasswordUIState(
                isCurrentPassword = true
            )
        )

        composeTestRule.onNodeWithTag(CHANGE_PASSWORD_BUTTON_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that when state is not current password should enable change password button`() {
        setComposeContent(
            ChangePasswordUIState(
                isCurrentPassword = false
            )
        )

        composeTestRule.onNodeWithTag(CHANGE_PASSWORD_BUTTON_TEST_TAG).assertIsEnabled()
    }

    @Test
    fun `test that when click change password button and not connected to network should show snackbar with no connection message`() {
        setComposeContent(ChangePasswordUIState(isConnectedToNetwork = false))

        composeTestRule.onNodeWithTag(CHANGE_PASSWORD_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasAnyChild(hasText(fromId(R.string.error_server_connection_problem))))
    }

    @Test
    fun `test that when click change password button and tnc not checked should show snackbar with tnc error message`() {
        setComposeContent(ChangePasswordUIState(isConnectedToNetwork = true))

        composeTestRule.onNodeWithTag(CHANGE_PASSWORD_BUTTON_TEST_TAG).performClick()

        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG)
            .assertIsDisplayed()
            .assert(hasAnyChild(hasText(fromId(R.string.create_account_no_top))))
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on light theme when password level is very weak`() {
        verifyBarStrength(strength = PasswordStrength.VERY_WEAK, color = red_600, isDark = false)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on dark theme when password level is very weak`() {
        verifyBarStrength(strength = PasswordStrength.VERY_WEAK, color = red_300, isDark = true)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on light theme when password level is weak`() {
        verifyBarStrength(strength = PasswordStrength.WEAK, color = yellow_600, isDark = false)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on dark theme when password level is weak`() {
        verifyBarStrength(strength = PasswordStrength.WEAK, color = yellow_300, isDark = true)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on light theme when password level is medium`() {
        verifyBarStrength(strength = PasswordStrength.MEDIUM, color = green_500, isDark = false)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on dark theme when password level is medium`() {
        verifyBarStrength(strength = PasswordStrength.MEDIUM, color = green_400, isDark = true)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on light theme when password level is good`() {
        verifyBarStrength(strength = PasswordStrength.GOOD, color = lime_green_500, isDark = false)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on dark theme when password level is good`() {
        verifyBarStrength(strength = PasswordStrength.GOOD, color = lime_green_200, isDark = true)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on light theme when password level is strong`() {
        verifyBarStrength(strength = PasswordStrength.STRONG, color = dark_blue_500, isDark = false)
    }

    @Test
    fun `test that password strength component has correct bar color and bar count on dark theme when password level is strong`() {
        verifyBarStrength(strength = PasswordStrength.STRONG, color = dark_blue_200, isDark = true)
    }

    private fun verifyBarStrength(strength: PasswordStrength, color: Color, isDark: Boolean) {
        composeTestRule.setContent {
            AndroidTheme(isDark = isDark) {
                PasswordStrengthBar(strengthAttribute = strength.toStrengthAttribute())
            }
        }

        for (index in 0..strength.value) {
            composeTestRule
                .onAllNodesWithTag(PASSWORD_STRENGTH_BAR_TEST_TAG)[index]
                .assert(hasBackgroundColor(color))
        }
    }
}