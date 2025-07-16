package mega.privacy.android.app.presentation.login.confirmemail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginViewModel

@Serializable
data object ConfirmationEmailScreen

internal fun NavGraphBuilder.confirmationEmailScreen(
    sharedViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
) {
    composable<ConfirmationEmailScreen> {
        NewConfirmEmailGraph(
            activityViewModel = sharedViewModel,
            onBackPressed = onBackPressed,
        )
    }
}

internal fun NavController.openConfirmationEmailScreen(navOptions: NavOptions? = null) {
    navigate(ConfirmationEmailScreen, navOptions)
}