package test.mega.privacy.android.app.presentation.testpassword

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import de.palm.composestateevents.triggered
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.app.presentation.testpassword.model.TestPasswordUIState
import mega.privacy.android.app.presentation.testpassword.view.Constants.BACKUP_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.CLOSE_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.CONFIRM_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.DISMISS_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_APP_BAR_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_DESC_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_DISMISS_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_LAYOUT_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_FOOTER_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PROCEED_TO_LOGOUT_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.SEE_PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.SNACKBAR_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_APP_BAR_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_LAYOUT_TAG
import mega.privacy.android.app.presentation.testpassword.view.TestPasswordComposeView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class TestPasswordComposeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(uiState: TestPasswordUIState = TestPasswordUIState()) {
        composeTestRule.setContent {
            TestPasswordComposeView(
                uiState = uiState,
                onBackupRecoveryClick = {},
                onResetUserMessage = {},
                onCheckCurrentPassword = {},
                onTestPasswordClick = {},
                onCheckboxValueChanged = {},
                onDismiss = {},
                onResetPasswordVerificationState = {},
                onUserLogout = {},
                onResetUserLogout = {},
                onFinishedCopyingRecoveryKey = {},
                onResetFinishedCopyingRecoveryKey = {},
                onExhaustedPasswordAttempts = {},
                onResetExhaustedPasswordAttempts = {}
            )
        }
    }

    @Test
    fun `test that on first load should render with correct state`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_LAYOUT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_APP_BAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_PASSWORD_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BACKUP_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DESC_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DISMISS_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that layout should render correct layout when test password mode and not logout mode`() {
        setComposeContent(TestPasswordUIState(isUITestPasswordMode = true))

        composeTestRule.onNodeWithTag(TEST_PASSWORD_LAYOUT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_PASSWORD_APP_BAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONFIRM_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BACKUP_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DISMISS_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROCEED_TO_LOGOUT_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that password reminder mode close icon should be visible when screen logout mode`() {
        setComposeContent(TestPasswordUIState(isLogoutMode = true))
        composeTestRule.onNodeWithTag(CLOSE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that password reminder mode close icon should be hidden when screen not logout mode`() {
        setComposeContent(TestPasswordUIState(isLogoutMode = false))
        composeTestRule.onNodeWithTag(CLOSE_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that password reminder description and button render with correct text when screen logout mode`() {
        setComposeContent(TestPasswordUIState(isLogoutMode = true))

        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DESC_TAG).assertIsDisplayed()
            .assert(hasText(fromId(R.string.remember_pwd_dialog_text_logout)))
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DISMISS_BUTTON_TAG).assertIsDisplayed()
            .assert(hasText(fromId(R.string.proceed_to_logout)))
    }

    @Test
    fun `test that password reminder description and button render with correct text when screen is not logout mode`() {
        setComposeContent(TestPasswordUIState(isLogoutMode = false))

        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DESC_TAG).assertIsDisplayed()
            .assert(hasText(fromId(R.string.remember_pwd_dialog_text)))
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DISMISS_BUTTON_TAG).assertIsDisplayed()
            .assert(hasText(fromId(R.string.general_dismiss)))
    }

    @Test
    fun `test that layout should render correct layout when test password mode and is logout mode`() {
        setComposeContent(TestPasswordUIState(isUITestPasswordMode = true, isLogoutMode = true))

        composeTestRule.onNodeWithTag(TEST_PASSWORD_LAYOUT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_PASSWORD_APP_BAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONFIRM_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BACKUP_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DISMISS_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PROCEED_TO_LOGOUT_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar will be shown when snackbar message not null`() {
        setComposeContent(TestPasswordUIState(userMessage = triggered(R.string.general_text_error)))

        composeTestRule.onNodeWithTag(SNACKBAR_TAG)
            .assertIsDisplayed()
            .assert(hasAnyChild(hasText(fromId(R.string.general_text_error))))
    }

    @Test
    fun `test that footer will show correct message when isCurrentPassword state is true`() {
        setComposeContent(
            TestPasswordUIState(
                isUITestPasswordMode = true,
                isCurrentPassword = PasswordState.True
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.test_pwd_accepted)))

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that footer will show correct message when isCurrentPassword state is false`() {
        setComposeContent(
            TestPasswordUIState(
                isUITestPasswordMode = true,
                isCurrentPassword = PasswordState.False
            )
        )

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_TAG)
            .assertIsDisplayed()
            .assert(hasText(fromId(R.string.test_pwd_wrong)))

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that footer should not be displayed when password state initial`() {
        setComposeContent(TestPasswordUIState(isCurrentPassword = PasswordState.Initial))

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that when text field is on focus should show see password icon`() {
        setComposeContent(TestPasswordUIState(isUITestPasswordMode = true))

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_TAG).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag(SEE_PASSWORD_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that password reminder ui should be disabled when loading`() {
        setComposeContent(TestPasswordUIState(isLoading = true))

        composeTestRule.onNodeWithTag(TEST_PASSWORD_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(BACKUP_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(PASSWORD_REMINDER_DISMISS_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `test that test password ui should be disabled when loading`() {
        setComposeContent(TestPasswordUIState(isUITestPasswordMode = true, isLoading = true))

        composeTestRule.onNodeWithTag(PASSWORD_TEXT_FIELD_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(CONFIRM_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(BACKUP_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithTag(DISMISS_BUTTON_TAG)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}