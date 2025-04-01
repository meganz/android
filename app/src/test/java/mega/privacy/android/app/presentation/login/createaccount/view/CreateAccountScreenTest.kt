package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountStatus
import mega.privacy.android.app.presentation.login.createaccount.model.CreateAccountUIState
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CreateAccountScreenTest {

    @get:Rule
    val composeRule = createComposeRule()


    @Test
    fun `test that screen title is displayed`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_TOOLBAR_TAG).assertIsDisplayed()
        }
    }


    @Test
    fun `test that screen title is not displayed when Create account is in progress`() {
        with(composeRule) {
            setScreen(state = CreateAccountUIState(isLoading = true))

            onNodeWithTag(CREATE_ACCOUNT_TOOLBAR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that MEGA logo is displayed when Create account is in progress`() {
        with(composeRule) {
            setScreen(state = CreateAccountUIState(isLoading = true))

            onNodeWithTag(CREATE_ACCOUNT_IN_PROGRESS_MEGA_LOGO_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that MEGA logo is not displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_IN_PROGRESS_MEGA_LOGO_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that progress bar is displayed when Create account is in progress`() {
        with(composeRule) {
            setScreen(state = CreateAccountUIState(isLoading = true))

            onNodeWithTag(CREATE_ACCOUNT_IN_PROGRESS_PROGRESS_INDICATOR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that progress bar is not displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_IN_PROGRESS_PROGRESS_INDICATOR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that first name input is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_FIRST_NAME_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that last name input is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_LAST_NAME_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that email input is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_EMAIL_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that password input is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_PASSWORD_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that confirm password input is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_CONFIRM_PASSWORD_TAG).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `test that create account button is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_CREATE_ACCOUNT_BUTTON_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that terms of service checkbox is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_TERMS_OF_SERVICE_CHECK_BOX_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that end to end encryption checkbox is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_END_TO_END_ENCRYPTION_CHECK_BOX_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that login button is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_LOGIN_BUTTON_TAG).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `test that terms of service text is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_TERMS_OF_SERVICE_TEXT_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that end to end encryption text is displayed for main create account screen`() {
        with(composeRule) {
            setScreen()

            onNodeWithTag(CREATE_ACCOUNT_END_TO_END_ENCRYPTION_TEXT_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that network warning is displayed when showNoNetworkWarning is true and there is no internet for main create account screen`() {
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(
                    showNoNetworkWarning = true,
                    isConnected = false
                )
            )

            onNodeWithTag(CREATE_ACCOUNT_NETWORK_WARNING_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that network warning is not displayed when showNoNetworkWarning is true but internet is detected for main create account screen`() {
        with(composeRule) {
            setScreen(state = CreateAccountUIState(showNoNetworkWarning = true, isConnected = true))

            onNodeWithTag(CREATE_ACCOUNT_NETWORK_WARNING_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that network warning is not displayed when showNoNetworkWarning is false for main create account screen`() {
        with(composeRule) {
            setScreen(state = CreateAccountUIState(showNoNetworkWarning = false))

            onNodeWithTag(CREATE_ACCOUNT_NETWORK_WARNING_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that onFirstNameInputChanged is invoked when first name changes for main create account screen`() {
        val firstNameChanged: (String) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onFirstNameInputChanged = firstNameChanged
            )

            val firstNameField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CREATE_ACCOUNT_FIRST_NAME_TAG))
                )

            firstNameField.performTextInput("First Name")
            verify(firstNameChanged).invoke("First Name")
        }
    }

    @Test
    fun `test that onLastNameInputChanged is invoked when last name changes for main create account screen`() {
        val lastNameChanged: (String) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onLastNameInputChanged = lastNameChanged
            )

            val lastNameField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CREATE_ACCOUNT_LAST_NAME_TAG))
                )

            lastNameField.performTextInput("Last Name")
            verify(lastNameChanged).invoke("Last Name")
        }
    }

    @Test
    fun `test that onEmailInputChanged is invoked when email changes for main create account screen`() {
        val emailChanged: (String) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onEmailInputChanged = emailChanged
            )

            val emailField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CREATE_ACCOUNT_EMAIL_TAG))
                )

            emailField.performTextInput("email@mega.co.nz")
            verify(emailChanged).invoke("email@mega.co.nz")
        }
    }

    @Test
    fun `test that onPasswordInputChanged is invoked when password changes for main create account screen`() {
        val passwordChanged: (String) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onPasswordInputChanged = passwordChanged
            )

            val passwordField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CREATE_ACCOUNT_PASSWORD_TAG)) and hasImeAction(
                        ImeAction.Next
                    ), useUnmergedTree = false
                )

            passwordField.performTextInput("password")
            verify(passwordChanged).invoke("password")
        }
    }


    @Test
    fun `test that onConfirmPasswordInputChanged is invoked when confirm password changes for main create account screen`() {
        val confirmPasswordChanged: (String) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onConfirmPasswordInputChanged = confirmPasswordChanged
            )

            val confirmPasswordField =
                composeRule.onNode(
                    hasAnyAncestor(hasTestTag(CREATE_ACCOUNT_CONFIRM_PASSWORD_TAG)) and hasImeAction(
                        ImeAction.Done
                    ), useUnmergedTree = false
                )

            confirmPasswordField.performTextInput("password")
            verify(confirmPasswordChanged).invoke("password")
        }
    }

    @Test
    fun `test that onCreateAccountClicked is invoked when create account button is clicked for main create account screen`() {
        val createAccountClicked: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                onCreateAccountClicked = createAccountClicked
            )
            composeRule.onNodeWithTag(
                CREATE_ACCOUNT_CREATE_ACCOUNT_BUTTON_TAG,
                useUnmergedTree = true
            )
                .performScrollTo().performClick()
            verify(createAccountClicked).invoke()
        }
    }

    @Test
    fun `test that onTermsOfServiceAgreedChanged is invoked when terms of service checkbox is clicked for main create account screen`() {
        val termsOfServiceAgreedChanged: (Boolean) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onTermsOfServiceAgreedChanged = termsOfServiceAgreedChanged
            )

            composeRule.onNodeWithTag(
                CREATE_ACCOUNT_TERMS_OF_SERVICE_CHECK_BOX_TAG,
                useUnmergedTree = true
            )
                .performScrollTo().performClick()
            verify(termsOfServiceAgreedChanged).invoke(true)
        }
    }

    @Test
    fun `test that onE2EEAgreedChanged is invoked when end to end encryption checkbox is clicked for main create account screen`() {
        val e2EEAgreedChanged: (Boolean) -> Unit = mock()
        with(composeRule) {
            setScreen(
                onE2EEAgreedChanged = e2EEAgreedChanged
            )

            composeRule.onNodeWithTag(
                CREATE_ACCOUNT_END_TO_END_ENCRYPTION_CHECK_BOX_TAG,
                useUnmergedTree = true
            )
                .performScrollTo().performClick()
            verify(e2EEAgreedChanged).invoke(true)
        }
    }

    @Test
    fun `test that onLoginClicked is invoked when login button is clicked`() {
        val loginClicked: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                onLoginClicked = loginClicked
            )

            composeRule.onNodeWithTag(CREATE_ACCOUNT_LOGIN_BUTTON_TAG, useUnmergedTree = true)
                .performScrollTo().performClick()
            verify(loginClicked).invoke()
        }
    }

    @Test
    fun `test that openTermsAndServiceLink is invoked when terms of service text is clicked`() {
        val openTermsAndServiceLink: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                openTermsAndServiceLink = openTermsAndServiceLink
            )

            composeRule.onNodeWithTag(
                CREATE_ACCOUNT_TERMS_OF_SERVICE_TEXT_TAG,
                useUnmergedTree = true
            )
                .performScrollTo().performClick()
            verify(openTermsAndServiceLink).invoke()
        }
    }

    @Test
    fun `test that openEndToEndEncryptionLink is invoked when end to end encryption text is clicked for main create account screen`() {
        val openEndToEndEncryptionLink: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                openEndToEndEncryptionLink = openEndToEndEncryptionLink
            )

            composeRule.onNodeWithTag(
                CREATE_ACCOUNT_END_TO_END_ENCRYPTION_TEXT_TAG,
                useUnmergedTree = true
            )
                .performScrollTo().performClick()
            verify(openEndToEndEncryptionLink).invoke()
        }
    }

    @Test
    fun `test that onResetShowAgreeToTermsEvent is invoked when showAgreeToTermsEvent is triggered for main create account screen`() {
        val resetShowAgreeToTermsEvent: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(showAgreeToTermsEvent = triggered),
                onResetShowAgreeToTermsEvent = resetShowAgreeToTermsEvent
            )
            verify(resetShowAgreeToTermsEvent).invoke()
        }
    }

    @Test
    fun `test that onResetCreateAccountStatusEvent is invoked when createAccountStatusEvent is triggered for main create account screen`() {
        val resetCreateAccountStatusEvent: () -> Unit = mock()
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(
                    createAccountStatusEvent = triggered(
                        CreateAccountStatus.AccountAlreadyExists
                    )
                ),
                onResetCreateAccountStatusEvent = resetCreateAccountStatusEvent
            )
            verify(resetCreateAccountStatusEvent).invoke()
        }
    }

    @Test
    fun `test that onCreateAccountSuccess is invoked when create account is successful for main create account screen`() {
        val createAccountSuccess: (EphemeralCredentials) -> Unit = mock()
        val ephemeralCredentials = mock<EphemeralCredentials>()
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(
                    createAccountStatusEvent = triggered(
                        CreateAccountStatus.Success(ephemeralCredentials)
                    )
                ),
                onCreateAccountSuccess = createAccountSuccess
            )
            verify(createAccountSuccess).invoke(ephemeralCredentials)
        }
    }

    @Test
    fun `test that passwo-rd strength views are displayed when password strength is valid and password is valid`() {
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(
                    passwordStrength = PasswordStrength.WEAK,
                    isPasswordValid = true
                )
            )

            onNodeWithTag(CREATE_ACCOUNT_PASSWORD_STRENGTH_VIEW_TAG).performScrollTo()
                .assertIsDisplayed()
            onNodeWithTag(CREATE_ACCOUNT_PASSWORD_STRENGTH_ADVICE_TEXT_TAG).performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that password strength views are displayed when password strength is not valid and password is invalid`() {
        with(composeRule) {
            setScreen(
                state = CreateAccountUIState(
                    passwordStrength = PasswordStrength.INVALID,
                    isPasswordValid = false
                )
            )

            onNodeWithTag(CREATE_ACCOUNT_PASSWORD_STRENGTH_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(CREATE_ACCOUNT_PASSWORD_STRENGTH_ADVICE_TEXT_TAG).assertDoesNotExist()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        state: CreateAccountUIState = CreateAccountUIState(),
        onFirstNameInputChanged: (String) -> Unit = {},
        onLastNameInputChanged: (String) -> Unit = {},
        onEmailInputChanged: (String) -> Unit = {},
        onPasswordInputChanged: (String) -> Unit = {},
        onConfirmPasswordInputChanged: (String) -> Unit = {},
        onCreateAccountClicked: () -> Unit = {},
        onTermsOfServiceAgreedChanged: (Boolean) -> Unit = {},
        onE2EEAgreedChanged: (Boolean) -> Unit = {},
        onLoginClicked: () -> Unit = {},
        openTermsAndServiceLink: () -> Unit = {},
        openEndToEndEncryptionLink: () -> Unit = {},
        onResetShowAgreeToTermsEvent: () -> Unit = {},
        onCloseNetworkWarningClick: () -> Unit = {},
        onResetCreateAccountStatusEvent: () -> Unit = {},
        onCreateAccountSuccess: (EphemeralCredentials) -> Unit = {},
        modifier: Modifier = Modifier,
    ) {
        setContent {
            CreateAccountScreen(
                uiState = state,
                onFirstNameInputChanged = onFirstNameInputChanged,
                onLastNameInputChanged = onLastNameInputChanged,
                onEmailInputChanged = onEmailInputChanged,
                onPasswordInputChanged = onPasswordInputChanged,
                onConfirmPasswordInputChanged = onConfirmPasswordInputChanged,
                onCreateAccountClicked = onCreateAccountClicked,
                onTermsOfServiceAgreedChanged = onTermsOfServiceAgreedChanged,
                onE2EEAgreedChanged = onE2EEAgreedChanged,
                onLoginClicked = onLoginClicked,
                openTermsAndServiceLink = openTermsAndServiceLink,
                openEndToEndEncryptionLink = openEndToEndEncryptionLink,
                onResetShowAgreeToTermsEvent = onResetShowAgreeToTermsEvent,
                onCloseNetworkWarningClick = onCloseNetworkWarningClick,
                onResetCreateAccountStatusEvent = onResetCreateAccountStatusEvent,
                onCreateAccountSuccess = onCreateAccountSuccess,
                modifier = modifier
            )
        }
    }
}
