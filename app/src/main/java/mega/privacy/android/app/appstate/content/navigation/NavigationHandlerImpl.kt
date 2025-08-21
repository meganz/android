package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation.NavHostController
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.mapNotNull
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

    override fun navigate(destination: NavKey) {
        navController.navigate(destination)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        navController.popBackStack(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: NavKey) {
        navController.navigate(destination) {
            popUpTo(0) { inclusive = true }
        }
    }

    override fun navigateAndClearTo(destination: NavKey, newParent: NavKey, inclusive: Boolean) {
        navController.navigate(destination) {
            popUpTo(newParent) { this.inclusive = inclusive }
        }
    }

    override fun <T> returnResult(key: String, value: T) {
        navController.previousBackStackEntry?.savedStateHandle?.set(key = key, value = value)
        navController.popBackStack()
    }

    override fun <T> monitorResult(key: String) =
        navController.currentBackStackEntryFlow.mapNotNull {
            if (it.savedStateHandle.contains(key)) {
                val result = it.savedStateHandle.get<T>(key)
                it.savedStateHandle.remove<T>(key)
                result
            } else null
        }
}