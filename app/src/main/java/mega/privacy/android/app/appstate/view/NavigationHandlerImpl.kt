package mega.privacy.android.app.appstate.view

import androidx.navigation.NavHostController
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Default implementation of NavigationHandler that wraps NavHostController functionality.
 */
class NavigationHandlerImpl(
    private val navController: NavHostController,
) : NavigationHandler {

    override fun back() {
        navController.popBackStack()
    }

    override fun navigate(destination: Any) {
        navController.navigate(destination)
    }

    override fun backTo(destination: Any, inclusive: Boolean) {
        navController.popBackStack(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: Any) {
        navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
        }
    }
}