package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.login.error
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.textfields.LabelTextField
import mega.privacy.android.core.ui.controls.textfields.PasswordTextField
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.account.AccountSession

/**
 * Login fragment view.
 */
@Composable
fun LoginView(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: (String?) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoginRequired) {
        RequireLogin(
            state = state,
            onEmailChanged = onEmailChanged,
            onPasswordChanged = onPasswordChanged,
            onLoginClicked = onLoginClicked,
            onForgotPassword = onForgotPassword,
            onCreateAccount = onCreateAccount,
            modifier = modifier
        )
    }
}

@Composable
private fun RequireLogin(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClicked: () -> Unit,
    onForgotPassword: (String?) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier) {
    val focusRequester = remember { FocusRequester() }

    Text(
        modifier = Modifier.padding(start = 22.dp, top = 17.dp, end = 22.dp),
        text = stringResource(id = R.string.login_to_mega),
        style = MaterialTheme.typography.subtitle1.copy(
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.textColorPrimary
        ),
    )
    LabelTextField(
        onTextChange = onEmailChanged,
        label = stringResource(id = R.string.email_text),
        imeAction = ImeAction.Next,
        keyboardActions = KeyboardActions(onNext = { focusRequester.requestFocus() }),
        modifier = Modifier.padding(start = 22.dp, top = 44.dp, end = 22.dp),
        text = state.accountSession?.email ?: "",
        errorText = state.emailError?.let { stringResource(id = it.error) }
    )
    PasswordTextField(
        onTextChange = onPasswordChanged,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { onLoginClicked() }),
        modifier = Modifier
            .padding(
                start = 22.dp,
                top = if (state.emailError != null) 24.dp else 44.dp,
                end = 22.dp
            )
            .focusRequester(focusRequester),
        text = state.password ?: "",
        errorText = state.passwordError?.let { stringResource(id = it.error) }
    )
    RaisedDefaultMegaButton(
        modifier = Modifier.padding(
            start = 22.dp,
            top = if (state.passwordError != null) 24.dp else 44.dp,
            end = 22.dp
        ),
        textId = R.string.login_text,
        onClick = onLoginClicked,
        enabled = !state.isLocalLogoutInProgress
    )
    Row(
        modifier = Modifier
            .padding(start = 22.dp, top = 10.dp, end = 22.dp)
            .alpha(if (state.isLocalLogoutInProgress) 1f else 0f)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .padding(start = 3.dp),
            color = MaterialTheme.colors.secondary,
            strokeWidth = 1.dp
        )
        Text(
            text = stringResource(id = R.string.login_in_progress),
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.secondary)
        )
    }
    TextMegaButton(
        modifier = Modifier.padding(start = 14.dp, top = 34.dp),
        textId = R.string.forgot_pass,
        onClick = { onForgotPassword(state.accountSession?.email) }
    )
    Row(modifier = Modifier.padding(end = 22.dp)) {
        Text(
            modifier = Modifier.padding(start = 22.dp, top = 18.dp),
            text = stringResource(id = R.string.new_to_mega),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorPrimary),
        )
        TextMegaButton(
            modifier = Modifier.padding(top = 4.dp),
            textId = R.string.create_account,
            onClick = onCreateAccount
        )
    }
}

@Composable
private fun LoginInProgress(
    state: LoginState,
    modifier: Modifier = Modifier,
) {

}

@Composable
private fun TwoFactorAuthentication(
    state: LoginState,
    modifier: Modifier = Modifier,
) {

}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewEmptyLoginView")
@Composable
private fun PreviewEmptyLoginView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var state by remember { mutableStateOf(LoginState(isLoginRequired = true)) }

        RequireLogin(
            state = state,
            onEmailChanged = { state = state.copy(accountSession = AccountSession(email = it)) },
            onPasswordChanged = { state = state.copy(password = it) },
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            modifier = Modifier.background(color = MaterialTheme.colors.surface)
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewLoginView")
@Composable
private fun PreviewLoginView(
    @PreviewParameter(LoginStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        RequireLogin(
            state = state,
            onEmailChanged = { },
            onPasswordChanged = { },
            onLoginClicked = {},
            onForgotPassword = {},
            onCreateAccount = {},
            modifier = Modifier.background(color = MaterialTheme.colors.surface)
        )
    }
}

/**
 * TextField parameter provider for compose previews.
 */
internal class LoginStateProvider : PreviewParameterProvider<LoginState> {
    override val values = listOf(
        LoginState(isLoginRequired = true),
        LoginState(
            isLoginRequired = true,
            accountSession = AccountSession(email = "email@email.es"),
            password = "Password",
            isLocalLogoutInProgress = true
        ),
        LoginState(
            isLoginRequired = true,
            accountSession = AccountSession(email = ""),
            emailError = LoginError.EmptyEmail,
            password = "",
            passwordError = LoginError.EmptyPassword
        )
    ).asSequence()
}
