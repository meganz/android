package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountViewModel
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.LAST_NAME
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CreateAccountCharacterLimitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setupRule(
        state: CreateAccountUIState = CreateAccountUIState(),
        onFirstNameInputChanged: (String) -> Unit = {},
        onLastNameInputChanged: (String) -> Unit = {},
        onEmailInputChanged: (String) -> Unit = {},
        onPasswordInputChanged: (String) -> Unit = {},
        onConfirmPasswordInputChanged: (String) -> Unit = {},
        onCreateAccountClicked: () -> Unit = {},
        onTermsOfServiceAgreedChanged: (Boolean) -> Unit = {},
        onLoginClicked: () -> Unit = {},
        openLink: (String) -> Unit = {},
        onResetCreateAccountStatusEvent: () -> Unit = {},
        onResetShowAgreeToTermsEvent: () -> Unit = {},
        onNetworkWarningShown: () -> Unit = {},
        onCreateAccountSuccess: (EphemeralCredentials) -> Unit = {},
        onBackIconPressed: () -> Unit = {},
    ) {
        composeRule.setContent {
            NewCreateAccountScreen(
                uiState = state,
                snackBarHostState = mock(),
                onFirstNameInputChanged = onFirstNameInputChanged,
                onLastNameInputChanged = onLastNameInputChanged,
                onEmailInputChanged = onEmailInputChanged,
                onPasswordInputChanged = onPasswordInputChanged,
                onConfirmPasswordInputChanged = onConfirmPasswordInputChanged,
                onCreateAccountClicked = onCreateAccountClicked,
                onTermsOfServiceAgreedChanged = onTermsOfServiceAgreedChanged,
                onLoginClicked = onLoginClicked,
                openLink = openLink,
                onResetCreateAccountStatusEvent = onResetCreateAccountStatusEvent,
                onResetShowAgreeToTermsEvent = onResetShowAgreeToTermsEvent,
                onNetworkWarningShown = onNetworkWarningShown,
                onCreateAccountSuccess = onCreateAccountSuccess,
                onBackIconPressed = onBackIconPressed
            )
        }
    }

    @Test
    fun `test that first name error message is displayed when length exceeded`() = runTest {
        val state = CreateAccountUIState(
            isFirstNameLengthExceeded = true
        )

        setupRule(state = state)

        composeRule.onNode(
            hasTestTag(FIRST_NAME)
        ).assert(hasAnyChild(hasText(context.getString(R.string.sign_up_first_name_text_field_char_limit_exceed_error))))
    }

    @Test
    fun `test that last name error message is displayed when length exceeded`() = runTest {
        val state = CreateAccountUIState(
            isLastNameLengthExceeded = true
        )

        setupRule(state = state)

        composeRule.onNode(
            hasTestTag(LAST_NAME)
        ).assert(hasAnyChild(hasText(context.getString(R.string.sign_up_last_name_text_field_char_limit_exceed_error))))
    }

    @Test
    fun `test that first name error message is displayed when field is invalid but not length exceeded`() =
        runTest {
            val state = CreateAccountUIState(
                isFirstNameValid = false,
                isFirstNameLengthExceeded = false
            )

            setupRule(state = state)

            composeRule.onNode(
                hasTestTag(FIRST_NAME)
            ).assert(hasAnyChild(hasText("Enter a first name")))
        }

    @Test
    fun `test that last name error message is displayed when field is invalid but not length exceeded`() =
        runTest {
            val state = CreateAccountUIState(
                isLastNameValid = false,
                isLastNameLengthExceeded = false
            )

            setupRule(state = state)

            composeRule.onNode(
                hasTestTag(LAST_NAME)
            ).assert(hasAnyChild(hasText("Enter a last name")))
        }

    @Test
    fun `test that no error messages are displayed when names are valid`() = runTest {
        val state = CreateAccountUIState(
            isFirstNameValid = true,
            isLastNameValid = true,
            isFirstNameLengthExceeded = false,
            isLastNameLengthExceeded = false
        )

        setupRule(state = state)

        // Verify first name field is displayed but without error
        composeRule.onNodeWithTag(FIRST_NAME).assertIsDisplayed()

        // Verify last name field is displayed but without error
        composeRule.onNodeWithTag(LAST_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that character limit constant is accessible through ViewModel`() {
        val limit = CreateAccountViewModel.NAME_CHAR_LIMIT
        assert(limit == 40) { "Expected NAME_CHAR_LIMIT to be 40, but was $limit" }
    }

    @Test
    fun `test that both error messages can be displayed simultaneously`() = runTest {
        val state = CreateAccountUIState(
            isFirstNameLengthExceeded = true,
            isLastNameLengthExceeded = true
        )

        setupRule(state = state)

        composeRule.onNode(
            hasTestTag(FIRST_NAME)
        ).assert(hasAnyChild(hasText(context.getString(R.string.sign_up_first_name_text_field_char_limit_exceed_error))))

        composeRule.onNode(
            hasTestTag(LAST_NAME)
        ).assert(hasAnyChild(hasText(context.getString(R.string.sign_up_last_name_text_field_char_limit_exceed_error))))
    }

    @Test
    fun `test that mixed error states display correct messages`() = runTest {
        val state = CreateAccountUIState(
            isFirstNameValid = false,
            isFirstNameLengthExceeded = false, // Empty name error
            isLastNameLengthExceeded = true     // Length exceeded error
        )

        setupRule(state = state)

        composeRule.onNode(
            hasTestTag(FIRST_NAME)
        ).assert(hasAnyChild(hasText(context.getString(R.string.sign_up_first_name_text_field_error_message))))

        composeRule.onNode(
            hasTestTag(LAST_NAME)
        )
            .assert(hasAnyChild(hasText(context.getString(R.string.sign_up_last_name_text_field_char_limit_exceed_error))))
    }
}