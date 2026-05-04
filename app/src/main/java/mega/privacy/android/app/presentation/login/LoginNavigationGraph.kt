package mega.privacy.android.app.presentation.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.login.confirmemail.confirmationEmailScreen
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.UpdateEmailForAccountCreationViewModel
import mega.privacy.android.app.presentation.login.confirmemail.updateEmail.updateEmailForAccountCreation
import mega.privacy.android.app.presentation.login.createaccount.createAccountScreen
import mega.privacy.android.app.presentation.login.onboarding.tourScreen
import mega.privacy.android.navigation.contract.NavigationHandler

internal fun EntryProviderScope<NavKey>.loginEntryProvider(
    navigationHandler: NavigationHandler,
    loginViewModel: LoginViewModel,
    onFinish: () -> Unit,
) {
    loginScreen(
        sharedViewModel = loginViewModel,
    )

    createAccountScreen(
        sharedViewModel = loginViewModel
    )

    tourScreen(
        sharedViewModel = loginViewModel,
        onBackPressed = onFinish,
    )

    confirmationEmailScreen(
        navigationHandler = navigationHandler,
        onFinish = onFinish,
        sharedViewModel = loginViewModel
    )

    updateEmailForAccountCreation(
        onChangeEmailSuccess = { newEmail ->
            navigationHandler.returnResult(
                key = UpdateEmailForAccountCreationViewModel.EMAIL,
                value = newEmail
            )
        },
    )
}
