package mega.privacy.android.app.presentation.login

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.billing.BillingViewModel

@Serializable
data object LoginScreen


internal fun NavGraphBuilder.loginScreen(
    sharedViewModel: LoginViewModel,
    billingViewModel: BillingViewModel
) {
    composable<LoginScreen> {
        LoginScreen(
            viewModel = sharedViewModel,
            billingViewModel = billingViewModel
        )
    }
}

internal fun NavController.openLoginScreen(navOptions: NavOptions? = null) {
    navigate(LoginScreen, navOptions)
}