package mega.privacy.android.app.presentation.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object LoginScreen


internal fun NavGraphBuilder.loginScreen() {
    composable<LoginScreen> {
        LoginScreen()
    }
}

internal fun NavController.openLoginScreen(navOptions: NavOptions? = null) {
    navigate(LoginScreen, navOptions)
}