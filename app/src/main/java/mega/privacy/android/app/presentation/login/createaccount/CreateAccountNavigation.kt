package mega.privacy.android.app.presentation.login.createaccount

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.createaccount.view.NewCreateAccountRoute

@Serializable
data object CreateAccountRoute

internal fun NavGraphBuilder.createAccountScreen(
    sharedViewModel: LoginViewModel,
) {
    composable<CreateAccountRoute> {
        NewCreateAccountRoute(
            activityViewModel = sharedViewModel,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal fun NavController.openCreateAccountScreen(navOptions: NavOptions? = null) {
    navigate(CreateAccountRoute, navOptions)
}