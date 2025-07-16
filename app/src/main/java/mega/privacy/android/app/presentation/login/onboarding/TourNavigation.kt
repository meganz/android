package mega.privacy.android.app.presentation.login.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute

@Serializable
data object TourScreen

internal fun NavGraphBuilder.tourScreen(
    sharedViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
) {
    composable<TourScreen> {
        NewTourRoute(
            activityViewModel = sharedViewModel,
            onBackPressed = onBackPressed,
        )
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourScreen, navOptions)
}