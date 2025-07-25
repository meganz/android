package mega.privacy.android.app.presentation.login.onboarding

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.presentation.login.LoginGraph
import mega.privacy.android.app.presentation.login.LoginGraphContent
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute

@Serializable
data object TourScreen

internal fun NavGraphBuilder.tourScreen(
    navController: NavController,
    chatRequestHandler: MegaChatRequestHandler,
    onFinish: () -> Unit,
    stopShowingSplashScreen: () -> Unit,
    activityViewModel: LoginViewModel? = null,
    onBackPressed: () -> Unit,
) {
    composable<TourScreen> { backStackEntry ->
        val sharedViewModel = activityViewModel ?: run {
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry<LoginGraph>()
            }
            hiltViewModel<LoginViewModel>(parentEntry)
        }
        LoginGraphContent(
            navController = navController,
            viewModel = sharedViewModel,
            chatRequestHandler = chatRequestHandler,
            onFinish = onFinish,
            stopShowingSplashScreen = stopShowingSplashScreen,
        ) {
            NewTourRoute(
                activityViewModel = sharedViewModel,
                onBackPressed = onBackPressed,
            )
        }
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourScreen, navOptions)
}