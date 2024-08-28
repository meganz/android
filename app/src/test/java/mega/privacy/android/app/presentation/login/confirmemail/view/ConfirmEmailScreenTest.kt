package mega.privacy.android.app.presentation.login.confirmemail.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ConfirmEmailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()


    @Test
    fun `test that screen title is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(TITLE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that confirm email icon is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(ICON_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that confirm email instruction is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(DESCRIPTION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that email address is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(EMAIL_ADDRESS_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that email address instruction is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(EMAIL_ADDRESS_DESCRIPTION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that resend button is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(RESEND_BUTTON_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that cancel button is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CANCEL_BUTTON_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct error message is shown when resending the confirmation link with a blank email`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(RESEND_BUTTON_TAG).performClick()

            onNodeWithText(R.string.error_enter_email).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct error message is shown when resending the confirmation link with an invalid email`() {
        with(composeRule) {
            setScreen(email = "email")

            onNodeWithTag(RESEND_BUTTON_TAG).performClick()

            onNodeWithText(R.string.error_invalid_email).assertIsDisplayed()
        }
    }

    @Test
    fun `test that successfully resending the confirmation link with a valid email when the user is connected to the network`() {
        with(composeRule) {
            var isResent = false
            setScreen(
                email = "email@email.com",
                uiState = ConfirmEmailUiState(isOnline = true),
                onResendSignUpLink = { isResent = true }
            )

            onNodeWithTag(RESEND_BUTTON_TAG).performClick()

            assertThat(isResent).isTrue()
        }
    }

    @Test
    fun `test that the offline message is shown when resending the confirmation link with a valid email and the user is not connected to the network`() {
        with(composeRule) {
            var isOfflineMessageShown = false
            setScreen(
                email = "email@email.com",
                onShowOfflineMessage = { isOfflineMessageShown = true }
            )

            onNodeWithTag(RESEND_BUTTON_TAG).performClick()

            assertThat(isOfflineMessageShown).isTrue()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        email: String = "",
        uiState: ConfirmEmailUiState = ConfirmEmailUiState(),
        onCancelClick: () -> Unit = {},
        onResendSignUpLink: (email: String) -> Unit = {},
        onShowOfflineMessage: () -> Unit = {},
    ) {
        setContent {
            ConfirmEmailScreen(
                email = email,
                uiState = uiState,
                onCancelClick = onCancelClick,
                onResendSignUpLink = onResendSignUpLink,
                onShowOfflineMessage = onShowOfflineMessage
            )
        }
    }
}
