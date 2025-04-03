package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.CONFIRM_PASSWORD
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.CREATE_ACCOUNT_BUTTON
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.EMAIL
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.FIRST_NAME
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.LAST_NAME
import mega.privacy.android.app.presentation.login.createaccount.view.CreateAccountTestTags.PASSWORD
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NewCreateAccountScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun `test that screen title is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CreateAccountTestTags.TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun `test that first name input is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CreateAccountTestTags.FIRST_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that last name input is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CreateAccountTestTags.LAST_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that email input is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CreateAccountTestTags.EMAIL).assertIsDisplayed()
    }

    @Test
    fun `test that password input is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(PASSWORD).assertIsDisplayed()
    }

    @Test
    fun `test that confirm password input is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CONFIRM_PASSWORD).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that create account button is displayed`() {
        setupRule()
        composeRule.onNodeWithTag(CREATE_ACCOUNT_BUTTON).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `test that onFirstNameInputChanged is invoked when first name changes`() {
        val firstNameChanged: (String) -> Unit = mock()
        with(composeRule) {
            setupRule(
                onFirstNameInputChanged = firstNameChanged
            )

            val firstNameField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(FIRST_NAME)) and hasImeAction(
                        ImeAction.Next
                    )
                )

            firstNameField.performTextInput("First Name")
            verify(firstNameChanged).invoke("First Name")
        }
    }

    @Test
    fun `test that onLastNameInputChanged is invoked when last name changes`() {
        val lastNameChanged: (String) -> Unit = mock()
        with(composeRule) {
            setupRule(
                onLastNameInputChanged = lastNameChanged
            )

            val lastNameField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(LAST_NAME)) and hasImeAction(
                        ImeAction.Next
                    )
                )

            lastNameField.performTextInput("Last Name")
            verify(lastNameChanged).invoke("Last Name")
        }
    }

    @Test
    fun `test that onEmailInputChanged is invoked when email changes`() {
        val onEmailInputChanged: (String) -> Unit = mock()
        val email = "email@mega.co.nz"
        with(composeRule) {
            setupRule(
                onEmailInputChanged = onEmailInputChanged
            )

            val emailField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(EMAIL)) and hasImeAction(
                        ImeAction.Next
                    )
                )

            emailField.performTextInput(email)
            verify(onEmailInputChanged).invoke(email)
        }
    }

    @Test
    fun `test that onPasswordInputChanged is invoked when password changes`() {
        val passwordChanged: (String) -> Unit = mock()
        val password = "password"
        with(composeRule) {
            setupRule(
                onPasswordInputChanged = passwordChanged
            )

            val passwordField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(PASSWORD)) and hasImeAction(
                        ImeAction.Next
                    )
                )

            passwordField.performTextInput(password)
            verify(passwordChanged).invoke(password)
        }
    }

    @Test
    fun `test that onConfirmPasswordInputChanged is invoked when confirm password changes`() {
        val confirmPasswordChanged: (String) -> Unit = mock()
        val confirmPassword = "password"
        with(composeRule) {
            setupRule(
                onConfirmPasswordInputChanged = confirmPasswordChanged
            )

            val confirmPasswordField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CONFIRM_PASSWORD)) and hasImeAction(
                        ImeAction.Done
                    )
                )

            confirmPasswordField.performTextInput(confirmPassword)
            verify(confirmPasswordChanged).invoke(confirmPassword)
        }
    }

    @Test
    fun `test that onCreateAccountClicked is invoked when create account button is clicked`() {
        val createAccountClicked: () -> Unit = mock()
        setupRule(onCreateAccountClicked = createAccountClicked)
        composeRule.onNodeWithTag(CreateAccountTestTags.CREATE_ACCOUNT_BUTTON).performScrollTo()
            .performClick()
        verify(createAccountClicked).invoke()
    }
}