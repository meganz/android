package mega.privacy.android.app.presentation.login.createaccount

import androidx.activity.compose.BackHandler
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CreateAccountScreen

internal fun NavGraphBuilder.createAccountScreen(
    onBackPressed: () -> Unit,
) {
    composable<CreateAccountScreen> {
        BackHandler(onBack = onBackPressed)
        AndroidFragment(CreateAccountComposeFragment::class.java)
    }
}

internal fun NavController.openCreateAccountScreen(navOptions: NavOptions? = null) {
    navigate(CreateAccountScreen, navOptions)
}