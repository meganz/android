package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.triggered
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChangeEmailAddressRouteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that the email change success event is emitted`() {
        val email = "email@email.email"
        with(composeRule) {
            val onChangeEmailSuccess = mock<(String) -> Unit>()
            setScreen(
                uiState = ChangeEmailAddressUIState(
                    email = email,
                    changeEmailAddressSuccessEvent = triggered
                ),
                onChangeEmailSuccess = onChangeEmailSuccess
            )

            verify(onChangeEmailSuccess).invoke(email)
        }
    }

    @Test
    fun `test that the top bar is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CHANGE_EMAIL_ADDRESS_SCREEN_TOP_BAR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the screen description is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CHANGE_EMAIL_ADDRESS_SCREEN_DESCRIPTION_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the email is updated when it is inputted`() {
        val email = "email@email.email"
        with(composeRule) {
            val onEmailInputChanged = mock<(String?) -> Unit>()
            setScreen(
                onEmailInputChanged = onEmailInputChanged
            )

            onNode(hasSetTextAction()).performTextInput(email)

            verify(onEmailInputChanged).invoke(email)
        }
    }

    @Test
    fun `test that the correct error message for an invalid email is displayed`() {
        with(composeRule) {
            setScreen(
                uiState = ChangeEmailAddressUIState(isEmailValid = false)
            )

            val text = context.getString(R.string.login_invalid_email_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct error message for account exists is displayed`() {
        with(composeRule) {
            setScreen(uiState = ChangeEmailAddressUIState(accountExistEvent = triggered))

            val text = context.getString(R.string.sign_up_account_existed_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the email is changed after clicking the update button`() {
        with(composeRule) {
            val onChangeEmailPressed = mock<() -> Unit>()
            setScreen(onChangeEmailPressed = onChangeEmailPressed)

            onNodeWithTag(CHANGE_EMAIL_ADDRESS_SCREEN_UPDATE_BUTTON_TAG).performClick()

            verify(onChangeEmailPressed).invoke()
        }
    }

    @Test
    fun `test that the loading indicator is displayed when content is loading`() {
        with(composeRule) {
            setScreen(uiState = ChangeEmailAddressUIState(isLoading = true))

            onNodeWithTag(CHANGE_EMAIL_ADDRESS_SCREEN_LOADING_INDICATOR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the loading indicator doesn't exist when content is not loading`() {
        with(composeRule) {
            setScreen(uiState = ChangeEmailAddressUIState(isLoading = false))

            onNodeWithTag(CHANGE_EMAIL_ADDRESS_SCREEN_LOADING_INDICATOR_TAG).assertDoesNotExist()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: ChangeEmailAddressUIState = ChangeEmailAddressUIState(),
        onEmailInputChanged: (String?) -> Unit = {},
        onChangeEmailPressed: () -> Unit = {},
        onResetGeneralErrorEvent: () -> Unit = {},
        onResetChangeEmailAddressSuccessEvent: () -> Unit = {},
        onResetAccountExistEvent: () -> Unit = {},
        onChangeEmailSuccess: (String) -> Unit = {},
    ) {
        setContent {
            ChangeEmailAddressScreen(
                uiState = uiState,
                onEmailInputChanged = onEmailInputChanged,
                onChangeEmailPressed = onChangeEmailPressed,
                onResetGeneralErrorEvent = onResetGeneralErrorEvent,
                onResetChangeEmailAddressSuccessEvent = onResetChangeEmailAddressSuccessEvent,
                onResetAccountExistEvent = onResetAccountExistEvent,
                onChangeEmailSuccess = onChangeEmailSuccess
            )
        }
    }
}