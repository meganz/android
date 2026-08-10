package mega.privacy.android.app.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.shared.resources.R

/**
 * QA Login View.
 * Simple login UI with email, password fields and login button.
 *
 * @param state Current login state
 * @param onEmailChanged Callback when email changes
 * @param onPasswordChanged Callback when password changes
 * @param onLoginClicked Callback when login button is clicked
 * @param onBackPressed Callback when back button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun QALoginView(
    state: QALoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val emailRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding(),
        topBar = {
            MegaTopAppBar(
                title = stringResource(id = R.string.login_text),
                navigationType = AppBarNavigationType.Back {
                    onBackPressed()
                    focusManager.clearFocus()
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.padding(16.dp))

            // Email field
            TextInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailRequester)
                    .focusProperties {
                        next = passwordRequester
                    },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.None,
                label = stringResource(id = R.string.email_text),
                text = state.email,
                onValueChanged = onEmailChanged,
                contentType = ContentType.EmailAddress,
            )

            Spacer(modifier = Modifier.padding(16.dp))

            // Password field
            PasswordTextInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordRequester)
                    .focusProperties {
                        previous = emailRequester
                    },
                label = stringResource(id = R.string.password_text),
                text = state.password,
                onValueChanged = onPasswordChanged,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onLoginClicked()
                    },
                ),
                errorText = state.errorMessage,
            )

            Spacer(modifier = Modifier.padding(24.dp))

            // Login button
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.login_text),
                isLoading = state.isLoading,
                enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
                onClick = {
                    focusManager.clearFocus()
                    onLoginClicked()
                },
            )
        }
    }

    // Auto focus email field on first composition
    LaunchedEffect(Unit) {
        emailRequester.requestFocus()
    }
}
